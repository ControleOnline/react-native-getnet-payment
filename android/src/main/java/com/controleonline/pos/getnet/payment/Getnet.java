package com.controleonline.pos.getnet.payment;

import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.BaseActivityEventListener;
import com.facebook.react.bridge.ReadableMap;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class Getnet extends ReactContextBaseJavaModule {
    private static final int REQUEST_CODE = 1001;
    private static final String SCHEME = "getnet";
    private static final String AUTHORITY = "pagamento";

    private Promise mPromise;
    private final ReactApplicationContext mContext;

    public Getnet(ReactApplicationContext reactContext) {
        super(reactContext);
        mContext = reactContext;
        mContext.addActivityEventListener(mActivityEventListener);
    }

    @Override
    public String getName() {
        return "Getnet";
    }

    /**
     * Formats amount in reais to 12-digit cents string.
     * Example: 15.00 -> "000000001500"
     */
    private String formatAmount(double amount) {
        long cents = Math.round(amount * 100);
        return String.format("%012d", cents);
    }

    @ReactMethod
    public void payment(ReadableMap params, Promise promise) {
        try {
            mPromise = promise;

            double amount = params.getDouble("amount");
            String paymentType = params.getString("paymentType");
            String callerId = params.getString("callerId");

            if (amount <= 0) {
                throw new Exception("Amount must be greater than zero");
            }
            if (paymentType == null || paymentType.isEmpty()) {
                throw new Exception("paymentType is required");
            }
            if (callerId == null || callerId.isEmpty()) {
                throw new Exception("callerId is required");
            }

            String amountFormatted = formatAmount(amount);

            Uri.Builder uriBuilder = new Uri.Builder()
                    .scheme(SCHEME)
                    .authority(AUTHORITY)
                    .appendPath("v3")
                    .appendPath("payment")
                    .appendQueryParameter("paymentType", paymentType)
                    .appendQueryParameter("amount", amountFormatted)
                    .appendQueryParameter("callerId", callerId);

            if (params.hasKey("installments")) {
                int installments = params.getInt("installments");
                if (installments > 0) {
                    uriBuilder.appendQueryParameter("installments", String.valueOf(installments));

                    if (installments > 1 && params.hasKey("creditType")) {
                        String creditType = params.getString("creditType");
                        if (creditType != null && !creditType.isEmpty()) {
                            uriBuilder.appendQueryParameter("creditType", creditType);
                        }
                    }
                }
            }

            if (params.hasKey("allowPrintCurrentTransaction")) {
                boolean allowPrint = params.getBoolean("allowPrintCurrentTransaction");
                uriBuilder.appendQueryParameter("allowPrintCurrentTransaction", String.valueOf(allowPrint));
            }

            if (params.hasKey("orderId")) {
                String orderId = params.getString("orderId");
                if (orderId != null && !orderId.isEmpty()) {
                    uriBuilder.appendQueryParameter("orderId", orderId);
                }
            }

            launchDeeplink(uriBuilder.build());
        } catch (Exception e) {
            rejectPromise(e.getMessage());
        }
    }

    @ReactMethod
    public void refund(ReadableMap params, Promise promise) {
        try {
            mPromise = promise;

            double amount = params.getDouble("amount");
            if (amount <= 0) {
                throw new Exception("Amount must be greater than zero");
            }

            String amountFormatted = formatAmount(amount);

            Uri.Builder uriBuilder = new Uri.Builder()
                    .scheme(SCHEME)
                    .authority(AUTHORITY)
                    .appendPath("v1")
                    .appendPath("refund")
                    .appendQueryParameter("amount", amountFormatted);

            if (params.hasKey("transactionDate")) {
                String transactionDate = params.getString("transactionDate");
                if (transactionDate != null && !transactionDate.isEmpty()) {
                    uriBuilder.appendQueryParameter("transactionDate", transactionDate);
                }
            }

            if (params.hasKey("cvNumber")) {
                String cvNumber = params.getString("cvNumber");
                if (cvNumber != null && !cvNumber.isEmpty()) {
                    uriBuilder.appendQueryParameter("cvNumber", cvNumber);
                }
            }

            if (params.hasKey("originTerminal")) {
                String originTerminal = params.getString("originTerminal");
                if (originTerminal != null && !originTerminal.isEmpty()) {
                    uriBuilder.appendQueryParameter("originTerminal", originTerminal);
                }
            }

            if (params.hasKey("allowPrintCurrentTransaction")) {
                boolean allowPrint = params.getBoolean("allowPrintCurrentTransaction");
                uriBuilder.appendQueryParameter("allowPrintCurrentTransaction", String.valueOf(allowPrint));
            }

            launchDeeplink(uriBuilder.build());
        } catch (Exception e) {
            rejectPromise(e.getMessage());
        }
    }

    @ReactMethod
    public void reprint(Promise promise) {
        try {
            mPromise = promise;

            Uri uri = new Uri.Builder()
                    .scheme(SCHEME)
                    .authority(AUTHORITY)
                    .appendPath("v1")
                    .appendPath("reprint")
                    .build();

            launchDeeplink(uri);
        } catch (Exception e) {
            rejectPromise(e.getMessage());
        }
    }

    @ReactMethod
    public void checkStatus(String callerId, Promise promise) {
        try {
            mPromise = promise;

            if (callerId == null || callerId.isEmpty()) {
                throw new Exception("callerId is required");
            }

            Uri uri = new Uri.Builder()
                    .scheme(SCHEME)
                    .authority(AUTHORITY)
                    .appendPath("v1")
                    .appendPath("checkstatus")
                    .appendQueryParameter("callerId", callerId)
                    .build();

            launchDeeplink(uri);
        } catch (Exception e) {
            rejectPromise(e.getMessage());
        }
    }

    private void launchDeeplink(Uri uri) throws Exception {
        final Activity activity = getCurrentActivity();

        if (activity == null) {
            throw new Exception("Activity not found");
        }

        Intent intent = new Intent(Intent.ACTION_VIEW, uri);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        activity.startActivityForResult(intent, REQUEST_CODE);
    }

    private void rejectPromise(String message) {
        if (mPromise != null) {
            WritableMap params = Arguments.createMap();
            params.putString("result", "3");
            params.putString("resultDetails", message != null ? message : "Unknown error");
            params.putBoolean("success", false);
            mPromise.resolve(params);
            mPromise = null;
        }
    }

    private WritableMap createParamsFromIntent(Intent data) {
        WritableMap params = Arguments.createMap();

        if (data == null) {
            params.putString("result", "2");
            params.putString("resultDetails", "Cancelled");
            params.putBoolean("success", false);
            return params;
        }

        String result = data.getStringExtra("result");
        params.putString("result", result != null ? result : "");
        params.putString("resultDetails", data.getStringExtra("resultDetails") != null ? data.getStringExtra("resultDetails") : "");
        params.putString("amount", data.getStringExtra("amount") != null ? data.getStringExtra("amount") : "");
        params.putString("callerId", data.getStringExtra("callerId") != null ? data.getStringExtra("callerId") : "");
        params.putString("nsu", data.getStringExtra("nsu") != null ? data.getStringExtra("nsu") : "");
        params.putString("nsuLastSuccesfullMessage", data.getStringExtra("nsuLastSuccesfullMessage") != null ? data.getStringExtra("nsuLastSuccesfullMessage") : "");
        params.putString("cvNumber", data.getStringExtra("cvNumber") != null ? data.getStringExtra("cvNumber") : "");
        params.putBoolean("receiptAlreadyPrinted", data.getBooleanExtra("receiptAlreadyPrinted", false));
        params.putString("type", data.getStringExtra("type") != null ? data.getStringExtra("type") : "");
        params.putString("brand", data.getStringExtra("brand") != null ? data.getStringExtra("brand") : "");
        params.putString("inputType", data.getStringExtra("inputType") != null ? data.getStringExtra("inputType") : "");
        params.putString("installments", data.getStringExtra("installments") != null ? data.getStringExtra("installments") : "");
        params.putString("gmtDateTime", data.getStringExtra("gmtDateTime") != null ? data.getStringExtra("gmtDateTime") : "");
        params.putString("nsuLocal", data.getStringExtra("nsuLocal") != null ? data.getStringExtra("nsuLocal") : "");
        params.putString("authorizationCode", data.getStringExtra("authorizationCode") != null ? data.getStringExtra("authorizationCode") : "");
        params.putString("cardBin", data.getStringExtra("cardBin") != null ? data.getStringExtra("cardBin") : "");
        params.putString("cardLastDigits", data.getStringExtra("cardLastDigits") != null ? data.getStringExtra("cardLastDigits") : "");
        params.putString("cardholderName", data.getStringExtra("cardholderName") != null ? data.getStringExtra("cardholderName") : "");
        params.putString("orderId", data.getStringExtra("orderId") != null ? data.getStringExtra("orderId") : "");
        params.putString("pixPayloadResponse", data.getStringExtra("pixPayloadResponse") != null ? data.getStringExtra("pixPayloadResponse") : "");
        params.putString("automationSlip", data.getStringExtra("automationSlip") != null ? data.getStringExtra("automationSlip") : "");
        params.putBoolean("printMerchantPreference", data.getBooleanExtra("printMerchantPreference", false));
        params.putBoolean("refund", data.getBooleanExtra("refund", false));

        // result "0" = success
        boolean success = "0".equals(result);
        params.putBoolean("success", success);

        return params;
    }

    private final ActivityEventListener mActivityEventListener = new BaseActivityEventListener() {
        @Override
        public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {
            if (requestCode != REQUEST_CODE) {
                return;
            }

            if (mPromise == null) {
                return;
            }

            if (resultCode == Activity.RESULT_OK) {
                WritableMap params = createParamsFromIntent(data);
                mPromise.resolve(params);
            } else {
                WritableMap params = Arguments.createMap();
                params.putString("result", "2");
                params.putString("resultDetails", "Cancelled or failed");
                params.putBoolean("success", false);
                mPromise.resolve(params);
            }

            mPromise = null;
        }
    };
}
