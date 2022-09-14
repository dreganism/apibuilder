package core

import org.scalatest.funspec.AnyFunSpec
import org.scalatest.matchers.should.Matchers

class ServiceMethodsSpec extends AnyFunSpec with Matchers {

  it("missing method") {
    val json = """
    {
      "name": "API Builder",
      "apidoc": { "version": "0.9.6" },

      "models": {
        "user": {
          "fields": [
            { "name": "id", "type": "long" }
          ]
        }
      },

      "resources": {
        "user": {
          "operations": [
            {}
          ]
        }
      }

    }
    """

    val validator = TestHelper.serviceValidatorFromApiJson(json)
    validator.errors().mkString("") should be("Resource[user] /users Missing method")
  }

}
