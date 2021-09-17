import org.scalatestplus.play.{HtmlUnitFactory, OneBrowserPerSuite, PlaySpec}
import controllers.HomeController
import controllers.actions.{AuthenticatedAction, NonAuthenticatedAction}
import models.CostsManagerModel
import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import play.api.test.{FakeRequest, Helpers}
import play.api.test.Helpers.baseApplicationBuilder.injector
import play.api.libs.concurrent.Futures
import play.api.test.Helpers.{contentAsString, defaultAwaitTimeout}

import scala.concurrent.ExecutionContext

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
        val registerLinkOption = find(cssSelector("a:nth-of-type(1)"))
        val forgotPasswordLinkOption = find(cssSelector("a:nth-of-type(2)"))

        registerLinkOption.nonEmpty mustBe true
        forgotPasswordLinkOption.nonEmpty mustBe true

        registerLinkOption.get.text mustBe "Register here"
        forgotPasswordLinkOption.get.text mustBe "Forgot password"
      }
    }

    "not be able to create another user with the same credentials provided" in {
      go to registerPageUrl

      pageTitle mustBe "Register page"
      fillRegisterPageFormAndSubmit(testUsername, testPassword, testEmail)

      eventually {
        pageTitle mustBe "Register page"
        currentUrl mustBe registerPageUrl
        val errorMessage = find(cssSelector("div span"))
        errorMessage.nonEmpty mustBe true
        errorMessage.get.text mustBe "Failed to create user"
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
