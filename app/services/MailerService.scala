package services

import play.api.libs.mailer.{Email, MailerClient}

import javax.inject.Inject

class MailerService @Inject()(mailerClient: MailerClient) {
  private val serviceMail = "scala@scalaproject.hit.ac.il"

  def sendSimpleEmail(emailTo: String, subject: String, content: String): String = {
    println(s"Sending mail to $emailTo")

    val email = Email(
      subject = subject,
      from = serviceMail,
      to = Seq(emailTo),
      bodyText = Some(content))

    mailerClient.send(email)
  }
}
