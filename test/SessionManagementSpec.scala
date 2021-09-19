import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import org.scalatestplus.play.{HtmlUnitFactory, OneBrowserPerSuite, PlaySpec}

class SessionManagementSpec extends PlaySpec with GuiceOneServerPerSuite with OneBrowserPerSuite with HtmlUnitFactory{
  val testUsername = "testUsername"
  val testPassword = "testPassword"
  val testEmail = "testEmail@gmail.com"
  val registerPageUrl = s"http://localhost:$port/register"
  val loginPageUrl = s"http://localhost:$port/login"
  val indexPageUrl = s"http://localhost:$port/"

  "HomeController" must {
    "be able to reach register page, register a new user and be redirected to the login page" in {
      go to registerPageUrl

      pageTitle mustBe "Register page"
      fillRegisterPageFormAndSubmit(testUsername, testPassword, testEmail)

      eventually {
        pageTitle mustBe "Login page"
        currentUrl mustBe loginPageUrl
        val registerLinkOption = find(cssSelector("div.login-buttons-grid a:nth-of-type(1) img"))
        val forgotPasswordLinkOption = find(cssSelector("div.login-buttons-grid a:nth-of-type(2) img"))

        registerLinkOption.nonEmpty mustBe true
        forgotPasswordLinkOption.nonEmpty mustBe true

        registerLinkOption.get.attribute("title").get mustBe "Register"
        forgotPasswordLinkOption.get.attribute("title").get mustBe "Forgot Password"
      }
    }

   "not be able to create another user with the same credentials provided" in {
      go to registerPageUrl

      pageTitle mustBe "Register page"
      fillRegisterPageFormAndSubmit(testUsername, testPassword, testEmail)

      eventually {
        pageTitle mustBe "Register page"
        currentUrl mustBe registerPageUrl
        val errorMessage = find(cssSelector("div.error-msg-grid span"))
        errorMessage.nonEmpty mustBe true
        errorMessage.get.text mustBe "Failed to create user"
      }
    }

    "fail upon trying to log in with wrong username or password" in {
      go to loginPageUrl
      pageTitle mustBe "Login page"

      fillLoginPageFormAndSubmit("wrongUsername", "somePassword")

      eventually {
        pageTitle mustBe "Login page"

        val errorMessage = find(cssSelector("div.error-msg-grid span"))

        errorMessage.nonEmpty mustBe true
        errorMessage.get.text mustBe "Wrong username or password"
      }
    }

    "be able to login through the login page and be redirected to the index page" in {
      go to loginPageUrl
      pageTitle mustBe "Login page"

      fillLoginPageFormAndSubmit(testUsername, testPassword)

      eventually {
        pageTitle mustBe "Home page"
        currentUrl mustBe indexPageUrl

        val welcomeMessage = find(cssSelector("h1"))

        welcomeMessage.nonEmpty mustBe true
        welcomeMessage.get.text mustBe "Your detailed costs"

        // new user must have empty cost details

        val emptyCostsDetails =  find(cssSelector("div.user-cost-details div"))

        emptyCostsDetails.nonEmpty mustBe true
        emptyCostsDetails.get.text mustBe "No details to display :("
      }
    }

    "be redirected to index when trying to go to register page" in {
      go to registerPageUrl

      pageTitle mustBe "Home page"
      currentUrl mustBe indexPageUrl
    }

    "be redirected to index when trying to go to login page" in {
      go to loginPageUrl

      pageTitle mustBe "Home page"
      currentUrl mustBe indexPageUrl
    }
  }

  def fillRegisterPageFormAndSubmit(username: String, password: String, email: String): Unit = {
    click on "username"
    textField("username").value = username
    click on "password"
    pwdField("password").value = password
    click on "email"
    emailField("email").value = email
    submit()
  }

  def fillLoginPageFormAndSubmit(username: String, password: String): Unit = {
    click on "username"
    textField("username").value = username
    click on "password"
    pwdField("password").value = password
    submit()
  }
}
