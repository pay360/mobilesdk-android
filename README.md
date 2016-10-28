# Pay360 Advanced Payments Android SDK

## Requirements

At minimum Android 4.0 (API level 14)

## Gradle

Add the following to the gradle build repositories

```groovy
    maven { url 'https://github.com/pay360/maven-repo/raw/master' }
```

And the following to the module gradle build dependencies

```groovy
    compile 'com.pay360:mobilesdk-android:1.0.0'
```

In the module gradle build set minSdkVersion to 14 or above.

## Manifest

Add the following to your AndroidManifest.xml

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.ACCESS_NETWORK_STATE" />

<activity android:name="com.pay360.sdk.library.ThreeDSActivity">
</activity>
```

## Register

You can sign up for a Pay360 Explorer account at [Pay360 Explorer](https://developer.paypoint.com/payments/explore/#/register).
This will give you access to a number of Pay360 products including the Mobile SDK in our MITE environment. MITE - Merchant Integration Test Environment - is a dedicated environment for merchants to explore our products and build their integration before go-live.  We'll send you an Installation ID for Cashier API once you have signed up which can be used with the Mobile SDK.
Payments made through the Mobile SDK can be tracked in our MITE [Portal](https://portal.mite.paypoint.net:3443/portal-client/#/en_gb/log_in)

## Testing your application in the MITE environment

In order to make payments in MITE your server must obtain a client access token.  The client access token will be used by your app to submit payments.
Instructions for doing this are available [here](https://developer.paypoint.com/payments/docs/#getting_started/using_our_sdks)

For convenience we provide a mock REST api which supplies these tokens for your MITE installations which can be used for prototyping your app.

## Mock Authorise Client Call

In MITE, you can use the following endpoint to return a client access token for use when making a payment

https://developer.paypoint.com/payments/explore/rest/mockmobilemerchant/getToken/<YOUR_INSTALLATION_ID>

## Making a Payment

Create a simple activity accepting a card number, expiry and CV2. Your activity will need to implement the PaymentManager.MakePaymentCallback interface.
Get an instance of PaymentManager in onCreate()

```java
paymentManager = PaymentManager.getInstance(this)
        .setUrl(EndpointManager.getEndpointUrl(EndpointManager.Environment.MITE));
```

Use EndpointManager.getEndpointUrl() to get the URL for a Pay360 environment.

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
        .setCurrency("GBP")
        .setAmount(10.54) // £10.54
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

To submit an Authorisation instead of a Payment, call setAuthorisation() on the transaction.

If this is the first payment or authorisation of a continuous authority sequence, you can indicate this using Transaction.setReccuring(). Subsequent repeats can be initiated using the "Repeat a Payment" call.
Details can be found here. https://developer.paypoint.com/payments/docs/#payments/repeat_a_payment

Financial services data and customer details can also optionally be created and set on the request.

For more details on the complete API see the [Javadocs](http://pay360.github.io/javadocs/mobilesdk-android/)

Validate the payment details handling the PaymentValidationException

```java
try {
    paymentManager.validatePaymentDetails(request);
} catch (PaymentValidationException e) {
  // handle validation errors
}
```

Note: the PaymentManager also provides static functions for inline validation of the card fields as they are being entered

```java
public static void validatePan(String pan) throws PaymentValidationException

public static void validateExpiry(String expiryDate) throws PaymentValidationException

public static void validateCv2(String cv2) throws PaymentValidationException
```

PaymentValidationException holds an error code enumeration describing the error.

If the PaymentRequest validates successfully i.e. does not throw a PaymentValidationException, your app should then communicate with **YOUR** server to request a client access token. This token, when returned, should be used to create a Credentials object which should then be passed to the PaymentManager.
If you're using MITE see 'Mock Authorise Client Call' section above.

```java
Credentials credentials = new Credentials()
        .setInstallationId((<YOUR_INSTALLATION_ID>);)
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

PaymentSuccess - has accessors for transaction id, merchant reference, amount, currency, last four digits of the card number and the masked card number.
PaymentError – contains a reasonCode and reasonMessage which can be used to feedback to the user.

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

A set of test cards for MITE are available here: [MITE test cards](https://developer.paypoint.com/payments/docs/#getting_started/test_cards)

## Javadoc

Javadocs can be found [here](http://pay360.github.io/javadocs/mobilesdk-android/)





