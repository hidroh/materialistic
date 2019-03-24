package io.github.hidroh.materialistic

import android.accounts.Account
import android.accounts.AccountManager
import android.content.Intent
import com.google.android.material.textfield.TextInputLayout
import android.view.View
import android.widget.EditText
import io.github.hidroh.materialistic.accounts.UserServices
import org.assertj.android.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.eq
import org.mockito.Captor
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.MockitoAnnotations
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowToast
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(application = InjectableApplication::class)
class LoginActivityTest {
  private lateinit var controller: ActivityController<TestLoginActivity>
  private lateinit var activity: TestLoginActivity
  @Captor
  private lateinit var callback: ArgumentCaptor<UserServices.Callback>

  @Before
  fun setUp() {
    MockitoAnnotations.initMocks(this)
    controller = Robolectric.buildActivity(TestLoginActivity::class.java)
    activity = controller.create().get()
  }

  @Test
  fun testEmptyLoginInput() {
    activity.findViewById<View>(R.id.login_button).performClick()
    assertThat(activity.findViewById<TextInputLayout>(R.id.textinput_username).error).isNotNull()
    assertThat(activity.findViewById<TextInputLayout>(R.id.textinput_password).error).isNotNull()
  }

  @Test
  fun testEmptyRegisterInput() {
    activity.findViewById<View>(R.id.register_button).performClick()
    assertThat(activity.findViewById<TextInputLayout>(R.id.textinput_username).error).isNotNull()
    assertThat(activity.findViewById<TextInputLayout>(R.id.textinput_password).error).isNotNull()
  }

  @Test
  fun testLoginSuccessful() {
    activity.findViewById<EditText>(R.id.edittext_username).setText("username")
    activity.findViewById<EditText>(R.id.edittext_password).setText("password")
    activity.findViewById<View>(R.id.login_button).performClick()
    assertThat(activity.findViewById<TextInputLayout>(R.id.textinput_username).error).isNull()
    assertThat(activity.findViewById<TextInputLayout>(R.id.textinput_password).error).isNull()
    verify(activity.userServices).login(eq("username"), eq("password"), eq(false), callback.capture())
    callback.value.onDone(true)
    assertThat(activity).isFinishing
    assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo(activity.getString(R.string.welcome, "username"))
    assertThat<Account>(AccountManager.get(activity).accounts).hasSize(1)
    assertThat(Preferences.getUsername(activity)).isEqualTo("username")
  }

  @Test
  fun testRegisterFailed() {
    activity.findViewById<EditText>(R.id.edittext_username).setText("username")
    activity.findViewById<EditText>(R.id.edittext_password).setText("password")
    activity.findViewById<View>(R.id.register_button).performClick()
    verify(activity.userServices).login(eq("username"), eq("password"), eq(true), callback.capture())
    callback.value.onDone(false)
    assertThat(activity).isNotFinishing
    assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo(activity.getString(R.string.login_failed))
  }

  @Test
  fun testLoginError() {
    activity.findViewById<EditText>(R.id.edittext_username).setText("username")
    activity.findViewById<EditText>(R.id.edittext_password).setText("password")
    activity.findViewById<View>(R.id.login_button).performClick()
    verify(activity.userServices).login(eq("username"), eq("password"), eq(false), callback.capture())
    callback.value.onError(IOException())
    assertThat(activity).isNotFinishing
    assertThat(ShadowToast.getTextOfLatestToast()).isEqualTo(activity.getString(R.string.login_failed))
  }

  @Test
  fun testReLogin() {
    controller = Robolectric.buildActivity(TestLoginActivity::class.java)
    Preferences.setUsername(RuntimeEnvironment.application, "existing")
    activity = controller.create().get()
    assertThat(activity).hasTitle(R.string.re_enter_password)
    assertThat(activity.findViewById<View>(R.id.register_button)).isNotVisible
    assertThat(activity.findViewById<EditText>(R.id.edittext_username))
        .hasTextString("existing")
  }

  @Test
  fun testAddAccount() {
    Preferences.setUsername(RuntimeEnvironment.application, "existing")
    controller = Robolectric.buildActivity(TestLoginActivity::class.java,
        Intent().putExtra(LoginActivity.EXTRA_ADD_ACCOUNT, true))
    activity = controller.create().get()
    assertThat(activity.findViewById<View>(R.id.register_button)).isVisible
    assertThat(activity.findViewById<EditText>(R.id.edittext_username)).isEmpty
  }

  @After
  fun tearDown() {
    controller.destroy()
  }

  class TestLoginActivity : LoginActivity() {
    val userServices = mock(UserServices::class.java)!!

    override fun inject(any: Any) {
      mUserServices = userServices
      mAccountManager = AccountManager.get(RuntimeEnvironment.application)
    }
  }
}
