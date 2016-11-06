package io.github.hidroh.materialistic.data;

import android.os.Parcel;

import org.junit.Test;
import org.junit.runner.RunWith;
import io.github.hidroh.materialistic.test.TestRunner;
import org.robolectric.RuntimeEnvironment;

import static junit.framework.Assert.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNotNull;

@RunWith(TestRunner.class)
public class UserItemTest {

    @Test
    public void testParcelReadWrite() {
        Parcel parcel = Parcel.obtain();
        parcel.writeString("username");
        parcel.writeLong(1);
        parcel.writeLong(2);
        parcel.writeLong(3);
        parcel.writeString("about");
        parcel.writeIntArray(new int[]{1, 2, 3});
        parcel.writeTypedArray(new HackerNewsItem[]{
                new HackerNewsItem(1L),
                new HackerNewsItem(2L),
                new HackerNewsItem(3L),
        }, 0);
        parcel.setDataPosition(0);

        UserItem actualRead = UserItem.CREATOR.createFromParcel(parcel);
        assertEquals("username", actualRead.getId());
        assertNotNull(actualRead.getCreated(RuntimeEnvironment.application));
        assertEquals(3, actualRead.getKarma());
        assertEquals("about", actualRead.getAbout());
        assertThat(actualRead.getItems()).hasSize(3);
        assertEquals(0, actualRead.describeContents());

        assertThat(UserItem.CREATOR.newArray(1)).hasSize(1);

        Parcel actualWrite = Parcel.obtain();
        actualRead.writeToParcel(actualWrite, 0);
        actualWrite.setDataPosition(0);
        assertEquals("username", actualWrite.readString());
        assertEquals(1, actualWrite.readLong());
        assertEquals(2, actualWrite.readLong());
        assertEquals(3, actualWrite.readLong());
        assertEquals("about", actualWrite.readString());
        assertThat(actualWrite.createIntArray()).hasSize(3);
        assertThat(actualWrite.createTypedArray(HackerNewsItem.CREATOR)).hasSize(3);
    }
}
