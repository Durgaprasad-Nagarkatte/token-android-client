/*
 * 	Copyright (c) 2017. Token Browser, Inc
 *
 * 	This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.tokenbrowser.presenter;

import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;

import com.tokenbrowser.R;
import com.tokenbrowser.exception.InvalidQrCode;
import com.tokenbrowser.exception.InvalidQrCodePayment;
import com.tokenbrowser.model.local.QrCodePayment;
import com.tokenbrowser.model.local.User;
import com.tokenbrowser.model.sofa.Payment;
import com.tokenbrowser.util.PaymentType;
import com.tokenbrowser.util.QrCode;
import com.tokenbrowser.util.QrCodeType;
import com.tokenbrowser.view.BaseApplication;
import com.tokenbrowser.view.activity.ChatActivity;
import com.tokenbrowser.view.activity.QrCodeHandlerActivity;
import com.tokenbrowser.view.activity.ViewUserActivity;
import com.tokenbrowser.view.fragment.DialogFragment.PaymentConfirmationDialog;

import rx.Single;
import rx.Subscription;
import rx.subscriptions.CompositeSubscription;

public class QrCodeHandlerPresenter implements
        Presenter<QrCodeHandlerActivity>,
        PaymentConfirmationDialog.OnPaymentConfirmationListener {

    private QrCodeHandlerActivity activity;
    private CompositeSubscription subscriptions;
    private boolean firstTimeAttaching = true;

    @Override
    public void onViewAttached(QrCodeHandlerActivity view) {
        this.activity = view;

        if (this.firstTimeAttaching) {
            this.firstTimeAttaching = false;
            initLongLivingObjects();
        }

        processIntentData();
    }

    private void initLongLivingObjects() {
        this.subscriptions = new CompositeSubscription();
    }

    private void processIntentData() {
        final Uri data = this.activity.getIntent().getData();
        handleIntentUri(data);
    }

    private void handleIntentUri(final Uri uri) {
        if (uri == null) return;

        final QrCode qrCode = new QrCode(uri.toString());
        final @QrCodeType.Type int qrCodeType = qrCode.getQrCodeType();

        if (qrCodeType == QrCodeType.EXTERNAL_PAY) {
            handleExternalPayment(qrCode);
        } else if (qrCodeType == QrCodeType.PAY) {
            handleTokenPayment(qrCode);
        } else if (qrCodeType == QrCodeType.ADD) {
            handleAddTokenUser(qrCode);
        }

        this.activity.getIntent().setData(null);
    }

    private void handleExternalPayment(final QrCode qrCode) {
        try {
            final QrCodePayment payment = qrCode.getExternalPayment();
            final Subscription sub =
                    BaseApplication
                    .get()
                    .getTokenManager()
                    .getUserManager()
                    .getUserFromPaymentAddress(payment.getAddress())
                    .subscribe(
                            user -> showTokenPaymentConfirmationDialog(user.getTokenId(), payment),
                            __ -> showExternalPaymentConfirmationDialog(payment)
                    );

            this.subscriptions.add(sub);
        } catch (InvalidQrCodePayment e) {
            handleInvalidQrCodePayment();
        }
    }

    private void showTokenPaymentConfirmationDialog(final String tokenId, final QrCodePayment payment) {
        if (this.activity == null) return;
        try {
            final PaymentConfirmationDialog dialog =
                    PaymentConfirmationDialog
                            .newInstanceTokenPayment(
                                    tokenId,
                                    payment.getValue(),
                                    payment.getMemo()
                            );
            dialog.show(this.activity.getSupportFragmentManager(), PaymentConfirmationDialog.TAG);
            dialog.setOnPaymentConfirmationListener(this);
        } catch (InvalidQrCodePayment e) {
            handleInvalidQrCodePayment();
        }
    }

    private void showExternalPaymentConfirmationDialog(final QrCodePayment payment) {
        if (this.activity == null) return;
        try {
            final PaymentConfirmationDialog dialog =
                    PaymentConfirmationDialog
                            .newInstanceExternalPayment(
                                    payment.getAddress(),
                                    payment.getValue(),
                                    payment.getMemo()
                            );
            dialog.show(this.activity.getSupportFragmentManager(), PaymentConfirmationDialog.TAG);
            dialog.setOnPaymentConfirmationListener(this);
        } catch (InvalidQrCodePayment e) {
            handleInvalidQrCodePayment();
        }
    }


    private void handleTokenPayment(final QrCode qrCode) {
        try {
            final QrCodePayment payment = qrCode.getPayment();
            final Subscription sub =
                    getUserByUsername(payment.getUsername())
                    .subscribe(
                            user -> showTokenPaymentConfirmationDialog(user.getTokenId(), payment),
                            __ -> handleInvalidQrCode()
                    );

            this.subscriptions.add(sub);
        } catch (InvalidQrCodePayment e) {
            handleInvalidQrCode();
        }
    }

    private void handleAddTokenUser(final QrCode qrCode) {
        try {
            final Subscription sub =
                    getUserByUsername(qrCode.getUsername())
                    .subscribe(
                            user -> goToProfileView(user.getTokenId()),
                            __ -> handleInvalidQrCode()
                    );

            this.subscriptions.add(sub);
        } catch (InvalidQrCode e) {
            handleInvalidQrCode();
        }
    }

    private Single<User> getUserByUsername(final String username) {
        return BaseApplication
                .get()
                .getTokenManager()
                .getUserManager()
                .getUserByUsername(username);
    }

    private void goToProfileView(final String tokenId) {
        if (this.activity == null) return;
        final Intent intent = new Intent(this.activity, ViewUserActivity.class)
                .putExtra(ViewUserActivity.EXTRA__USER_ADDRESS, tokenId)
                .putExtra(ViewUserActivity.EXTRA__PLAY_SCAN_SOUNDS, true);
        this.activity.startActivity(intent);
        this.activity.finish();
    }

    private void handleInvalidQrCode() {
        Toast.makeText(this.activity, this.activity.getString(R.string.invalid_qr_code), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPaymentRejected() {
        this.activity.finish();
    }

    @Override
    public void onTokenPaymentApproved(final String tokenId, final Payment payment) {
        try {
            goToChatActivityWithPayment(tokenId, payment);
        } catch (InvalidQrCodePayment e) {
            handleInvalidQrCodePayment();
        }
    }

    private void goToChatActivityWithPayment(final String tokenId, final Payment payment) throws InvalidQrCodePayment {
        final Intent intent = new Intent(activity, ChatActivity.class)
                .putExtra(ChatActivity.EXTRA__REMOTE_USER_ADDRESS, tokenId)
                .putExtra(ChatActivity.EXTRA__PAYMENT_ACTION, PaymentType.TYPE_SEND)
                .putExtra(ChatActivity.EXTRA__ETH_AMOUNT, payment.getValue())
                .putExtra(ChatActivity.EXTRA__PLAY_SCAN_SOUNDS, true);

        this.activity.startActivity(intent);
        this.activity.finish();
    }

    private void handleInvalidQrCodePayment() {
        Toast.makeText(this.activity, this.activity.getString(R.string.invalid_payment), Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onExternalPaymentApproved(final Payment payment) {
        try {
            sendExternalPayment(payment);
        } catch (InvalidQrCodePayment invalidQrCodePayment) {
            handleInvalidQrCodePayment();
        }
    }

    private void sendExternalPayment(final Payment payment) throws InvalidQrCodePayment {
        BaseApplication
                .get()
                .getTokenManager()
                .getTransactionManager()
                .sendExternalPayment(payment.getToAddress(), payment.getValue());
    }

    @Override
    public void onViewDetached() {
        this.subscriptions.clear();
        this.activity = null;
    }

    @Override
    public void onDestroyed() {
        this.subscriptions = null;
        this.activity = null;
    }
}
