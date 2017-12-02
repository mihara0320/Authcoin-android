package com.authcoinandroid.ui.fragment;

import android.os.Bundle;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import com.authcoinandroid.R;
import com.authcoinandroid.exception.RegisterEirException;
import com.authcoinandroid.service.identity.IdentityService;
import com.authcoinandroid.service.identity.WalletService;
import com.authcoinandroid.service.qtum.SendRawTransactionResponse;
import com.authcoinandroid.ui.activity.MainActivity;
import org.bitcoinj.wallet.UnreadableWalletException;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

import java.util.ArrayList;
import java.util.List;

public class NewIdentityFragment extends Fragment {
    private final static String LOG_TAG = "NewIdentityFragment";
    private List<EditText> identifierFields = new ArrayList<>();

    @BindView(R.id.et_alias)
    EditText alias;

    @BindView(R.id.et_identifier_1)
    EditText firstIdentifier;

    public NewIdentityFragment() {
    }

    @OnClick({R.id.btn_add_identifier})
    public void onAddIdentifier(View view) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        TextInputLayout newTextInputLayout = (TextInputLayout) inflater.inflate(R.layout.material_field_text, null, false);
        LinearLayout identifiersLayout = (LinearLayout) getActivity().findViewById(R.id.identifiers_wrapper);
        newTextInputLayout.setHint(getResources().getString(R.string.identifier) + " " + String.valueOf(identifiersLayout.getChildCount()));
        identifierFields.add(newTextInputLayout.getEditText());
        identifiersLayout.addView(newTextInputLayout);
    }

    @OnClick({R.id.btn_add_identity})
    public void onAddIdentity(View view) {
        Log.d(LOG_TAG, "Added identity");

        if (validateAlias()) {
            List<String> list = new ArrayList<>();
            for (EditText identifier : identifierFields) {
                String value = identifier.getText().toString();
                if (!value.isEmpty()) {
                    list.add(value);
                }
            }
            String[] identifiers = list.toArray(new String[0]);
            registerEir(identifiers, alias.getText().toString());
        }
    }

    private boolean validateAlias() {
        if (alias.getText().toString().isEmpty()) {
            Log.d(LOG_TAG, "Alias was empty");
            alias.setError("Alias must be filled");
            return false;
        }
        return true;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.new_identity_fragment, container, false);
        ButterKnife.bind(this, view);
        identifierFields.add(firstIdentifier);
        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        getActivity().findViewById(R.id.bottom_navigation).setVisibility(View.VISIBLE);
    }

    private void registerEir(String[] identifiers, String alias) {
        try {
            IdentityService.getInstance()
                    .registerEirWithEcKey(
                            WalletService.getInstance().getReceiveKey(this.getContext()),
                            identifiers,
                            alias)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe(new Subscriber<SendRawTransactionResponse>() {
                        @Override
                        public void onCompleted() {
                        }

                        @Override
                        public void onError(Throwable e) {
                            Log.d(LOG_TAG, e.getMessage());
                        }

                        @Override
                        public void onNext(SendRawTransactionResponse response) {
                            Log.d(LOG_TAG, response.getTxid() + " - " + response.getResult());
                            ((MainActivity) getActivity()).applyFragment(IdentityFragment.class, false, false);
                        }
                    });
        } catch (RegisterEirException | UnreadableWalletException e) {
            ((MainActivity) getActivity()).displayError(LOG_TAG, e.getMessage());
        }
    }
}