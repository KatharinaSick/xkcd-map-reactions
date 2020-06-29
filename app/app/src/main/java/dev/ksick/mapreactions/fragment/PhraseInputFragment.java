package dev.ksick.mapreactions.fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.fragment.NavHostFragment;

import dev.ksick.mapreactions.R;

public class PhraseInputFragment extends Fragment {

    private EditText etPhrase = null;

    @Override
    public View onCreateView(
            LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState
    ) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_phrase_input, container, false);
    }

    public void onViewCreated(@NonNull View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etPhrase = view.findViewById(R.id.et_phrase);
        view.findViewById(R.id.button_go).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onClickedGo();
            }
        });
        view.findViewById(R.id.button_info).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onClickedInfo();
            }
        });
        etPhrase.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                etPhrase.setError(null);
            }

            @Override
            public void afterTextChanged(Editable editable) {

            }
        });
        etPhrase.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_GO) {
                    onClickedGo();
                    return true;
                }
                return false;
            }
        });
    }

    public void onClickedGo() {
        if (etPhrase.getText() == null || etPhrase.getText().toString().trim().isEmpty()) {
            etPhrase.setError(getString(R.string.please_add_input));
            return;
        }

        Bundle arguments = new Bundle();
        arguments.putString(MapFragment.ARG_NAME_PHRASE, etPhrase.getText().toString().trim());

        etPhrase.setText(null);

        NavHostFragment
                .findNavController(PhraseInputFragment.this)
                .navigate(R.id.action_PhraseInputFragment_to_MapFragment, arguments);
    }

    public void onClickedInfo() {

    }
}