import org.scalatestplus.play.guice.GuiceOneServerPerSuite
import org.scalatestplus.play.{HtmlUnitFactory, OneBrowserPerSuite, PlaySpec}

class WebBrowserSpec extends PlaySpec with GuiceOneServerPerSuite with OneBrowserPerSuite with HtmlUnitFactory {
  val testUsername = "testUsername"
  val testPassword = "testPassword"
  val testEmail = "testEmail@gmail.com"
  val registerPageUrl = s"http://localhost:$port/register"
  val loginPageUrl = s"http://localhost:$port/login"
  val indexPageUrl = s"http://localhost:$port/"
  val addItemCostPage = s"http://localhost:$port/addItemCost"

  "SessionManagement" must {
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

        val emptyCostsDetails = find(cssSelector("div.user-cost-details div"))

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

  "UserItemCosts" must {
    "add a new item to the user item costs" in {
      go to addItemCostPage
      pageTitle mustBe "Add cost"

      fillAddCostFormAndSubmit("Pizza", "2020-04-04T13:55", "Food", "55")

      eventually {
        currentUrl mustBe addItemCostPage
        pageTitle mustBe "Add cost"

        val addedSuccessfullyMessage = find(cssSelector("div span"))

        addedSuccessfullyMessage.nonEmpty mustBe true
        addedSuccessfullyMessage.get.text mustBe "Cost added successfully!"
      }
    }

    "get an error message upon submitting a form with an empty itemName field" in {
      fillAddCostFormAndSubmit("", "2020-04-04T13:55", "Food", "55")

      eventually {
        currentUrl mustBe addItemCostPage
        pageTitle mustBe "Add cost"

        val addedSuccessfullyMessage = find(cssSelector("div span"))

        addedSuccessfullyMessage.nonEmpty mustBe true
        addedSuccessfullyMessage.get.text mustBe "Item name must not be empty"
      }
    }

    "get an error message upon submitting a form with an empty itemPrice field" in {
      fillAddCostFormAndSubmit("Pizza", "2020-04-04T13:55", "Food", "")

      eventually {
        currentUrl mustBe addItemCostPage
        pageTitle mustBe "Add cost"

        val addedSuccessfullyMessage = find(cssSelector("div span"))

        addedSuccessfullyMessage.nonEmpty mustBe true
        addedSuccessfullyMessage.get.text mustBe "Item price must be a numeric type"
      }
    }

    "load index page and view added item" in {
      go to indexPageUrl

      val costDetailsTable = find(cssSelector("table"))
      costDetailsTable.nonEmpty mustBe true

      val firstItemName = find(cssSelector("tr[id='1'] td:nth-of-type(1)"))
      val firstPurchaseDate = find(cssSelector("tr[id='1'] td:nth-of-type(2)"))
      val firstCategory = find(cssSelector("tr[id='1'] td:nth-of-type(3)"))
      val firstItemPrice = find(cssSelector("tr[id='1'] td:nth-of-type(4)"))

      firstItemName.nonEmpty mustBe true
      firstItemName.get.text mustBe "Pizza"

      firstPurchaseDate.nonEmpty mustBe true
      firstPurchaseDate.get.text mustBe "2020-04-04 13:55"

      firstCategory.nonEmpty mustBe true
      firstCategory.get.text mustBe "Food"

      firstItemPrice.nonEmpty mustBe true
      firstItemPrice.get.text mustBe "55"
    }
  }

  private def fillRegisterPageFormAndSubmit(username: String, password: String, email: String): Unit = {
    click on "username"
    textField("username").value = username
    click on "password"
    pwdField("password").value = password
    click on "email"
    emailField("email").value = email
    submit()
  }

  private def fillLoginPageFormAndSubmit(username: String, password: String): Unit = {
    click on "username"
    textField("username").value = username
    click on "password"
    pwdField("password").value = password
    submit()
  }

  private def fillAddCostFormAndSubmit(itemName: String, purchaseDate: String, category: String, itemPrice: String): Unit = {
    click on "itemName"
    textField("itemName").value = itemName
    click on "purchaseDate"
    dateTimeLocalField("purchaseDate").value = purchaseDate
    click on "category"
    singleSel("category").value = category
    click on "itemPrice"
    numberField("itemPrice").value = itemPrice
    submit()
  }
}
