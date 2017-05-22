## Synthetic methods

- what the heck are synthetic methods and why do they matter?

It starts here:

https://github.com/bumptech/glide/issues/1555

and even earlier from Jake Wharton:

https://realm.io/news/360andev-jake-wharton-java-hidden-costs-android/

Found this discussion:

http://stackissue.com/bumptech/glide/removed-synthetic-methods-1556.html

<pre>
Stack Issue
Removed synthetic methods #1556
Project glide

RiteshChandnani
## Description
This removes all the synthetic methods.
Issue #1555
## Motivation and Context

This change helps us reduce the method count and therefore also helps us avoid the trampoline created because of synthetic methods.
TWiStErRob
Looking at the diff I think it would be a good idea to leave /*private*/ in the code and put a comment in the default constructors so that a future cleanup wouldn't reverse the changes.

How does adding a default scoped ctor help? Without a default scoped ctor (1 method) a private one is generated with an accessor (2 methods)?

The 26 PMD violations are likely `UnnecessaryConstructor`s.
RiteshChandnani
@TWiStErRob
Thanks for checking my PR.
I will put a comment in default constructors to avoid cleanup. But, I can't get my arms around - " I think it would be a good idea to leave /private/ in the code", can you please elaborate a bit more.

And, yes, if we don't have a default scoped ctor, two ctors will be synthesized, one will be the private constructor with no params and one will be a package private constructor with an arbitrary param (its type is dependent on compiler implementation, and hence one addtional empty class to support ctor overloading.
I also made a dummy implementation and used reflection and found the same.

Sorry, due to different timezones I was unable to get back to you immediately.
TWiStErRob
> I think it would be a good idea to leave `/*private*/` in the code

If you look at this code out of context, what you see is that one of the fields are non-private for seemingly no reason (all usages of it are within the same class).

```java
private final Glide glide;
final Lifecycle lifecycle;
private final RequestTracker requestTracker;
```

This would make it more clear that it was supposed to be private, but cannot due to some limitation.
```java
private final Glide glide;
/*private*/ final Lifecycle lifecycle;
private final RequestTracker requestTracker;
```
potentially an even better approach would be a `@PreventSynthetic` or `@Private` source-level annotation to prevent thinking of commenting out as a forget-to-revert-after-debug thing.

Actually, thinking of it, isn't there a library out there which does this automatically? I mean if there is a way to have lambdas in Java 5, some APT code should be able to increase visibility of fields.
RiteshChandnani
Got it.
So, we can have a `@PreventSynthetic` source-level annotation to make the code more clear.

Like this one:
https://github.com/hidroh/materialistic/blob/master/app/src/main/java/io/github/hidroh/materialistic/annotation/Synthetic.java

So, I can go forward and implement that source-level annotation and update the code accordingly?

Yeah, there should be a library which does this automatically, but I was unable to find something after doing a quick search.
TWiStErRob
Yep, that example looks good, hidroh/materialistic#642 is where it was introduced.

So the changes are:
```diff
- private final Type name;
+ @Synthetic final Type name;
```
I like the shorter `@Synthetic` name. Annotate the constructors as well, then they can be left empty.
TWiStErRob
Btw, don't know what @sjudd's stand is on this, I hope he'll accept, but not sure.
RiteshChandnani
Great.
Will do it right away!

Yeah, even I hope the same about that.
RiteshChandnani
Btw, I think we should create a new directory named "annotation" in "glide/library/src/main/java/com/bumptech/glide/"

What's your take?
RiteshChandnani
Also, `diskLruCache` is a git submodule, can I go and update that one too?
TWiStErRob
Yes, you can contribute to `disklrucache` as well, it's http://github.com/sjudd/disklrucache that you need to fork. You need two separate PRs (this and another) and you can't depend on code in each other (i.e. the annotation). As far as I remember the cache doesn't have many classes, so it should be less likely to have synthetics.

I would put the annotation in `library/src/main/java/com/bumptech/glide/util`, because it's not part of the public API.
RiteshChandnani
@TWiStErRob
Got it.
Thanks!
I will first finish with this PR and later look into `disklrucache`.

Will move the annotation directory under `library/src/main/java/com/bumptech/glide/util`
© Copyright stackissue.com, All rights reserved.
</pre>