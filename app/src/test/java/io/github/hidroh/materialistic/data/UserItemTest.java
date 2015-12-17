package io.github.hidroh.materialistic.data;

import android.os.Parcel;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricGradleTestRunner;

import static junit.framework.Assert.assertEquals;
import static org.assertj.core.api.Assertions.assertThat;

@RunWith(RobolectricGradleTestRunner.class)
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
        parcel.writeTypedArray(new HackerNewsClient.HackerNewsItem[]{
                new HackerNewsClient.HackerNewsItem(1L),
                new HackerNewsClient.HackerNewsItem(2L),
                new HackerNewsClient.HackerNewsItem(3L),
        }, 0);
        parcel.setDataPosition(0);

        HackerNewsClient.UserItem actualRead = HackerNewsClient.UserItem.CREATOR.createFromParcel(parcel);
        assertEquals("username", actualRead.getId());
        assertEquals(2, actualRead.getCreated());
        assertEquals(3, actualRead.getKarma());
        assertEquals("about", actualRead.getAbout());
        assertThat(actualRead.getItems()).hasSize(3);
        assertEquals(0, actualRead.describeContents());

        assertThat(HackerNewsClient.UserItem.CREATOR.newArray(1)).hasSize(1);

        Parcel actualWrite = Parcel.obtain();
        actualRead.writeToParcel(actualWrite, 0);
        actualWrite.setDataPosition(0);
        assertEquals("username", actualWrite.readString());
        assertEquals(1, actualWrite.readLong());
        assertEquals(2, actualWrite.readLong());
        assertEquals(3, actualWrite.readLong());
        assertEquals("about", actualWrite.readString());
        assertThat(actualWrite.createIntArray()).hasSize(3);
        assertThat(actualWrite.createTypedArray(HackerNewsClient.HackerNewsItem.CREATOR)).hasSize(3);
    }
}
