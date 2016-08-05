package com.ftsafe.iccd.ecard.ui.fragments;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.GridView;

import com.ftsafe.iccd.ecard.R;
import com.ftsafe.iccd.ecard.ui.adapters.GridItemAdapter;

/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link FirstFragment.OnFirstFragmentInteractionListener} interface
 * to handle interaction events.
 */
public class FirstFragment extends Fragment implements AdapterView.OnItemClickListener, View.OnClickListener {

    private OnFirstFragmentInteractionListener mListener;
    // UI
    private GridView mGridView;
    private Button mOtgBtn, mBleBtn, mNfcBtn;

    public FirstFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(com.ftsafe.iccd.ecard.R.layout.fragment_first, container, false);
        // button
        mOtgBtn = (Button) view.findViewById(R.id.otg_btn);
        mOtgBtn.setOnClickListener(this);

        mBleBtn = (Button) view.findViewById(R.id.ble_btn);
        mBleBtn.setOnClickListener(this);

        mNfcBtn = (Button) view.findViewById(R.id.nfc_btn);
        mNfcBtn.setOnClickListener(this);


        // grid view
        mGridView = (GridView) view.findViewById(com.ftsafe.iccd.ecard.R.id.gridview);
        mGridView.setAdapter(new GridItemAdapter(getActivity()));

        // set grid view itemclickLisenter
        mGridView.setOnItemClickListener(this);

        changeButtonAlpha(100);
        return view;
    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction4First(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFirstFragmentInteractionListener) {
            mListener = (OnFirstFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFirstFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        position += 10;
        if (mListener != null)
            mListener.onButtonClick4First(position);
    }

    @Override
    public void onClick(View v) {
        changeButtonAlpha(100);
        if (v == mOtgBtn) {
            //mOtgBtn.setBackgroundColor(getResources().getColor(R.color.colorAccent));
            mOtgBtn.getBackground().setAlpha(255);
            mListener.onButtonClick4First(0);
        } else if (v == mBleBtn) {
            //mBleBtn.setBackgroundColor(getResources().getColor(R.color.colorAccent));
            mBleBtn.getBackground().setAlpha(255);
            mListener.onButtonClick4First(1);
        } else if (v == mNfcBtn) {
            //mNfcBtn.setBackgroundColor(getResources().getColor(R.color.colorAccent));
            mNfcBtn.getBackground().setAlpha(255);
            mListener.onButtonClick4First(2);
        } else {
            return;
        }

    }

    private void changeButtonAlpha(int alpha) {
        mOtgBtn.getBackground().setAlpha(alpha);
        mBleBtn.getBackground().setAlpha(alpha);
        mNfcBtn.getBackground().setAlpha(alpha);
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFirstFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction4First(Uri uri);

        void onButtonClick4First(int position);
    }
}
