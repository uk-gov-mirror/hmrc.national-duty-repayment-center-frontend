package connectors


import support.AppISpec


class UpscanInitiateConnectorISpec extends UpscanInitiateConnectorISpecSetup {

  "UpscanInitiateConnector" when {
    "/upscan/v2/initiate" should {
      "return upload request metadata" in {

        //givenUpscanInitiateSucceeds("https://myservice.com/callback")
        //givenAuditConnector()

        //val result: UpscanInitiateResponse =
         // await(connector.initiate(UpscanInitiateRequest(callbackUrl = "https://myservice.com/callback")))

        //result.reference shouldBe "11370e18-6e24-453e-b45a-76d3e32ea33d"
        //result.uploadRequest.href shouldBe testUploadRequest.href
        //result.uploadRequest.fields.toSet should contain theSameElementsAs (testUploadRequest.fields.toSet)

      }
    }
  }

}

trait UpscanInitiateConnectorISpecSetup extends AppISpec {

  //implicit val hc: HeaderCarrier = HeaderCarrier()

  //override def fakeApplication: Application = appBuilder.build()


}
