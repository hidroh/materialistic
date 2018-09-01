package io.github.hidroh.materialistic

import android.widget.TextView
import org.assertj.android.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = InjectableApplication::class)
class AboutActivityTest {
  @Test
  fun test() {
    val activity = Robolectric.buildActivity(AboutActivity::class.java).create().get()
    assertThat(activity.findViewById<TextView>(R.id.text_application_info)).containsText("Version")
    assertThat(activity.findViewById<TextView>(R.id.text_developer_info)).containsText("Ha Duy Trung")
    assertThat(activity.findViewById<TextView>(R.id.text_libraries)).isNotEmpty
    assertThat(activity.findViewById<TextView>(R.id.text_license)).containsText("Apache")
  }
}
