package io.github.hidroh.materialistic.data;

import android.os.Parcel;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import io.github.hidroh.materialistic.test.TestRunner;
import org.robolectric.RuntimeEnvironment;

import io.github.hidroh.materialistic.R;
import io.github.hidroh.materialistic.test.TestItem;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertTrue;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(TestRunner.class)
public class HackerNewsItemTest {
    private HackerNewsItem item;

    @Before
    public void setUp() {
        item = new HackerNewsItem(1l);
    }

    @Test
    public void testId() {
        assertEquals("1", item.getId());
    }

    @Test
    public void testFavorite() {
        assertFalse(item.isFavorite());
        item.setFavorite(true);
        assertTrue(item.isFavorite());
    }

    @Test
    public void testViewed() {
        assertFalse(item.isViewed());
        item.setIsViewed(true);
        assertTrue(item.isViewed());
    }

    @Test
    public void testLocalRevision() {
        assertEquals(-1, item.getLocalRevision());
        item.setLocalRevision(0);
        assertEquals(0, item.getLocalRevision());
    }

    @Test
    public void testCollapsed() {
        assertFalse(item.isCollapsed());
        item.setCollapsed(true);
        assertTrue(item.isCollapsed());
    }

    @Test
    public void testPopulate() {
        item.populate(new TestItem() {
            @Override
            public String getTitle() {
                return "title";
            }

            @Override
            public String getRawType() {
                return "rawType";
            }

            @Override
            public String getRawUrl() {
                return "rawUrl";
            }

            @Override
            public long[] getKids() {
                return new long[]{1l};
            }

            @Override
            public String getBy() {
                return "by";
            }

            @Override
            public long getTime() {
                return 1234l;
            }

            @Override
            public String getText() {
                return "text";
            }

            @Override
            public String getParent() {
                return "1";
            }

            @Override
            public boolean isDead() {
                return true;
            }

            @Override
            public boolean isDeleted() {
                return true;
            }

            @Override
            public int getDescendants() {
                return 1;
            }

            @Override
            public int getScore() {
                return 5;
            }
        });
        assertEquals("title", item.getTitle());
        assertEquals("text", item.getText());
        assertEquals("rawType", item.getRawType());
        assertEquals("rawUrl", item.getRawUrl());
        assertEquals("by", item.getBy());
        assertEquals(1234l, item.getTime());
        assertEquals(1, item.getDescendants());
        assertEquals(1, item.getLastKidCount());
        assertEquals(5, item.getScore());
        assertTrue(item.isDead());
        assertTrue(item.isDeleted());
        assertThat(item.getDisplayedAuthor(RuntimeEnvironment.application, true, 0))
                .contains(" - by");
        assertThat(item.getDisplayedTime(RuntimeEnvironment.application))
                .isNotEmpty();
        assertThat(item.getKids()).hasSize(1);
        assertEquals(1, item.getKidItems()[0].getLevel());
    }

    @Test
    public void testGetTypeDefault() {
        item.populate(new TestItem() {
            @Override
            public String getRawType() {
                return null;
            }
        });
        assertEquals(Item.STORY_TYPE, item.getType());
    }

    @Test
    public void testGetType() {
        item.populate(new TestItem() {
            @Override
            public String getRawType() {
                return "poll";
            }
        });
        assertEquals(Item.POLL_TYPE, item.getType());
    }

    @Test
    public void testGetTypeInvalid() {
        item.populate(new TestItem() {
            @Override
            public String getRawType() {
                return "blah";
            }
        });
        assertEquals("blah", item.getType());
    }

    @Test
    public void testGetDisplayedTitleComment() {
        item.populate(new TestItem() {
            @Override
            public String getRawType() {
                return "comment";
            }

            @Override
            public String getText() {
                return "comment";
            }
        });
        assertEquals("comment", item.getDisplayedTitle());
    }

    @Test
    public void testGetDisplayedTitleNonComment() {
        item.populate(new TestItem() {
            @Override
            public String getRawType() {
                return "story";
            }

            @Override
            public String getTitle() {
                return "title";
            }
        });
        assertEquals("title", item.getDisplayedTitle());
    }

    @Test
    public void testGetDisplayedTime() {
        item.populate(new TestItem() {
            @Override
            public long getTime() {
                return 1429027200l; // Apr 15 2015
            }

            @Override
            public String getBy() {
                return "author";
            }
        });
        assertThat(item.getDisplayedAuthor(RuntimeEnvironment.application, true, 0))
                .contains(" - author");
        assertThat(item.getDisplayedAuthor(RuntimeEnvironment.application, false, 0))
                .contains(" - author");
        item.populate(new TestItem() {
            @Override
            public String getBy() {
                return "author";
            }

            @Override
            public boolean isDead() {
                return true;
            }
        });
        assertThat(item.getDisplayedTime(RuntimeEnvironment.application))
                .contains(RuntimeEnvironment.application.getString(R.string.dead_prefix));
    }

    @Test
    public void testKidCount() {
        item.populate(new TestItem() {
            @Override
            public int getDescendants() {
                return 10;
            }
        });
        assertEquals(10, item.getKidCount());
    }

    @Test
    public void testLastKidCount() {
        item.populate(new TestItem() {
            @Override
            public int getDescendants() {
                return 0;
            }
        });
        assertEquals(0, item.getLastKidCount());
        item.setLastKidCount(1);
        assertEquals(1, item.getLastKidCount());
    }

    @Test
    public void testHasNewKids() {
        assertFalse(item.hasNewKids());
        item.populate(new TestItem() {
            @Override
            public int getDescendants() {
                return 0;
            }
        });
        assertFalse(item.hasNewKids());
        item.populate(new TestItem() {
            @Override
            public int getDescendants() {
                return 1;
            }
        });
        assertTrue(item.hasNewKids());
        item.populate(new TestItem() {
            @Override
            public int getDescendants() {
                return 1;
            }
        });
        assertFalse(item.hasNewKids());
    }

    @Test
    public void testKidCountNull() {
        item.populate(new TestItem() {
            @Override
            public long[] getKids() {
                return null;
            }
        });
        assertEquals(0, item.getKidCount());
    }

    @Test
    public void testKidCountNoDescendants() {
        item.populate(new TestItem() {
            @Override
            public long[] getKids() {
                return new long[]{1l, 2l};
            }
        });
        assertEquals(2, item.getKidCount());
    }

    @Test
    public void testGetUrlStory() {
        item.populate(new TestItem() {
            @Override
            public String getRawType() {
                return "story";
            }

            @Override
            public String getRawUrl() {
                return "http://example.com";
            }
        });
        assertEquals("http://example.com", item.getUrl());
    }

    @Test
    public void testGetUrlStoryNoUrl() {
        item.populate(new TestItem() {
            @Override
            public String getRawType() {
                return "story";
            }
        });
        assertEquals(String.format(HackerNewsClient.WEB_ITEM_PATH, "1"), item.getUrl());
    }

    @Test
    public void testGetUrlNonStory() {
        item.populate(new TestItem() {
            @Override
            public String getRawType() {
                return "comment";
            }
        });
        assertEquals(String.format(HackerNewsClient.WEB_ITEM_PATH, "1"), item.getUrl());
    }

    @Test
    public void testGetSource() {
        assertEquals("news.ycombinator.com", item.getSource());
        item.populate(new TestItem() {
            @Override
            public String getRawUrl() {
                return "http://example.com";
            }
        });
        assertEquals("example.com", item.getSource());
    }

    @Test
    public void testGetKidItems() {
        assertThat(item.getKidItems()).isEmpty();
        item.populate(new TestItem() {
            @Override
            public long[] getKids() {
                return new long[]{1l, 2l};
            }
        });
        assertThat(item.getKidItems()).hasSize(2);
        assertEquals(1, item.getKidItems()[0].getRank());
        assertEquals(2, item.getKidItems()[1].getRank());
    }

    @Test
    public void testIsShareable() {
        assertTrue(item.isStoryType());
        item.populate(new TestItem() {
            @Override
            public String getRawType() {
                return "comment";
            }
        });
        assertFalse(item.isStoryType());
        item.populate(new TestItem() {
            @Override
            public String getRawType() {
                return "poll";
            }
        });
        assertTrue(item.isStoryType());
    }

    @Test
    public void testParcelReadWrite() {
        Parcel parcel = Parcel.obtain();
        item.populate(new TestItem() {
            @Override
            public String getTitle() {
                return "title";
            }
        });
        item.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        Item actual = HackerNewsItem.CREATOR.createFromParcel(parcel);
        assertEquals("title", actual.getDisplayedTitle());
        assertFalse(actual.isFavorite());
    }

    @Test
    public void testParcelFavorite() {
        Parcel parcel = Parcel.obtain();
        item.populate(new TestItem() {
        });
        item.setFavorite(true);
        item.writeToParcel(parcel, 0);
        parcel.setDataPosition(0);
        assertTrue(HackerNewsItem.CREATOR.createFromParcel(parcel).isFavorite());
    }

    @Test
    public void testParcelable() {
        assertThat(HackerNewsItem.CREATOR.newArray(1)).hasSize(1);
        assertEquals(0, item.describeContents());
    }

    @Test
    public void testEquals() {
        assertFalse(item.equals(null));
        assertFalse(item.equals(new TestItem(){}));
        assertFalse(item.equals(new HackerNewsItem(2l)));
        assertTrue(item.equals(item));
        assertTrue(item.equals(new HackerNewsItem(1l)));
    }
}
