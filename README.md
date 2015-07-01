# PayPoint Advanced Payments Android SDK

## Requirements

At minimum Android 4.0 (API level 14)

## Download

Add the following to the gradle build repositories

```groovy
    maven { url 'https://github.com/paypoint/maven-repo/raw/master' }
```

And the following to the gradle build dependencies

```groovy
    compile 'net.paypoint:mobilesdk-android:1.0.0'
```

In the module gradle build set minSdkVersion to 14 or above.

Add the following to your AndroidManifest.xml

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<activity android:name="com.paypoint.sdk.library.ThreeDSActivity"
    android:screenOrientation="portrait">
</activity>
```

## Register

Register for an account at [PayPoint Explorer](https://developer.paypoint.com/payments/explore/#/register)
This will provide installation ids for Hosted Cashier and Cashier API, either can be used with the Mobile SDK.
Payments made through the Mobile SDK can be tracked in the [Portal](https://portal.mite.paypoint.net:3443/portal-client/#/en_gb/log_in)

## Testing your application in the MITE environment

PayPoint provide a Merchant Integration and Testing Environment (MITE), which lets you test your payment applications. In order to make test payments your server must obtain a client access token for your app, from our API. Instructions for doing this are available here:
TBD: {TODO: placeholder for server-side authoriseClient call}
For convenience we provide a mock REST api which supplies these tokens for your test installations which can be used for prototyping your app in our MITE environment:

## Mock Authorise Client Call

For testing against MITE, you can use the following endpoint to return an authorisation token for use when making a payment

https://developer.paypoint.com/payments/explore/rest/mockmobilemerchant/getToken/<YOUR_PAYPOINT_INSTALLATION_ID>

## Making a Payment

Create a simple activity accepting a card number, expiry and CV2.
Get an instance of PaymentManager in onCreate()

```java
paymentManager = PaymentManager.getInstance(this)
        .setUrl(EndpointManager.getEndpointUrl(EndpointManager.Environment.MITE));
```

Use EndpointManager.getEndpointUrl() to get the URL for a PayPoint environment.

Register a payment callback handler in OnResume and unregister the callback in OnPause to ensure your activity handles device orientation changes correctly if not locked to a single orientation.

```java
@Override
protected void onPause() {
    super.onPause();

    paymentManager.lockCallback();
    paymentManager.unregisterPaymentCallback();
}

@Override
protected void onResume() {
    super.onResume();

    paymentManager.registerPaymentCallback(this);
    paymentManager.unlockCallback();
}
```

In your payment button handler build a PaymentRequest

```java
PaymentCard card = new PaymentCard()
        .setPan("2470456729287342")
        .setExpiryDate("0116")
        .setCv2("457")
        .setCardHolderName("Mr A Smith");

Transaction transaction = new Transaction()
        .setCurrency(“GBP”)
        .setAmount("10.54")
        .setMerchantReference(merchantRef); // up to merchant to create a unique merchantRef

BillingAddress address = new BillingAddress()
        .setLine1("House Name")
        .setLine2("Street")
        .setCity("Bath")
        .setRegion("Somerset")
        .setPostcode("BA1 5BG");

// create the payment request
request = new PaymentRequest()
        .setCard(card)
        .setTransaction(transaction)
        .setAddress(address);
```

You may also want to provide custom fields

```java
List<CustomField> customFields = new ArrayList<CustomField>();

customFields.add(new CustomField()
    .setName("CustomName")
    .setValue("CustomValue")
    .setTransient(true));

customFields.add(new CustomField()
    .setName("AnotherCustomName")
    .setValue("AnotherCustomValue")
    .setTransient(false));

request.setCustomFields(customFields);
```

To submit an Authorisation instead of a Payment call setAuthorisation() on the transaction.

If this is the first payment or authorisation of a continuous authority sequence, you can indicate this using Transaction.setReccuring(). Subsequent repeats can be initiated using the "Repeat a Payment" call.
Details can be found here. https://developer.paypoint.com/payments/docs/#payments/repeat_a_payment

Financial services data and customer details can also optionally be created and set on the request.

Your activity will need to implement the PaymentManager.MakePaymentCallback interface.

Validate the payment details handling the PaymentValidationException

```java
paymentManager.validatePaymentDetails(request);
```

Note: the PaymentManager also provides static functions for inline validation of the card fields as they are being entered

```java
public static void validatePan(String pan) throws PaymentValidationException

public static void validateExpiry(String expiryDate) throws PaymentValidationException

public static void validateCv2(String cv2) throws PaymentValidationException
```

PaymentValidationException holds an error code enumeration describing the error.

If the PaymentRequest validates successfully i.e. does not throw a PaymentValidationException, your app should then communicate with **YOUR** server to request a PayPoint authorisation token. This token, when returned, should be used to create a PayPointCredentials object which should then be passed to the PaymentManager.
For testing against MITE see 'Mock Authorise Client Call' section above.

```java
PayPointCredentials credentials = new PayPointCredentials()
        .setInstallationId((<YOUR_PAYPOINT_INSTALLATION_ID>);)
        .setToken(token);

paymentManager.setCredentials(credentials);
```

Next, make the payment by calling makePayment() on the PaymentManager passing the request storing the returned operation identifier should you wish to retrieve the status of the transaction at a later point, see Error Handling.

```java
operationId = paymentManager.makePayment(request);
```

This call to makePayment() will callback to your app when completed in one of the following functions

```java
public void paymentSucceeded(PaymentSuccess paymentSuccess)

public void paymentFailed(PaymentError paymentError)
```

PaymentSuccess - has accessors for transaction id, merchant reference, amount, currency and last four digits of the card number.
PaymentError – use getKind() to return the type of error. PayPoint errors contain a reasonCode and reasonMessage which can be used to feedback to the user.

If a payment requires 3D Secure, an activity is automatically presented full screen. This activity will consists of a form that the user is expected to complete. Once the user completes this process the activity will dismiss and the payment will proceed.

NOTE - the SDK will always callback within a set timeout period (defaulted to 60s). If you wish to change the timeout period call PaymentManager.setSessionTimeout().
Care should be taken when setting this value as short timeouts might not allow enough time for the payment to be authorised.
This timeout does not include any delays resulting from the user being redirected to 3D Secure.

## Error Handling

If a payment fails e.g. SDK calls back into paymentFailed(), there will be instances where the payment is in an indeterminate\unknown state i.e. the transaction times out or a network error occurred.

If isSafeToRetryPayment() on the reasonCode returns true then it is safe call makePayment again to retry the payment. Otherwise you should query the state of the last transaction by calling getTransactionStatus passing the operation identifier returned by makePayment.

The function getTransactionStatus will use the same callback mechanism as makePayment().

## Get Status of a Previous Transaction

Use PaymentManager.getTransactionStatus to retrieve the status of a previous transaction, passing in the operation id generated by makePayment.
The function getTransactionStatus will use the same callback mechanism as makePayment().

## Test Cards

In the MITE environment you can use the standard test PANs for testing your applications (including 3DS test cards):
[MITE test cards](https://developer.paypoint.com/payments/docs/#getting_started/test_cards)

## Javadoc

Javadocs can be found [here](http://paypoint.github.io/javadocs/mobilesdk-android/)





