package pe.com.e2i.e2iapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
//import android.app.Fragment;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;

public class MainFragment extends Fragment {

    private final String LOG_TAG = MainFragment.class.getSimpleName();

    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private Button mReferenceImageButton;
    private Button mCameraButton;
    private TextView mTextViewCheck;
    private TextView mTextViewCheckX;

//    private OnFragmentInteractionListener mListener;

    /**
     * A callback interface that all activities containing this fragment must
     * implement. This mechanism allows activities to be notified of item
     * selections.
     */
    public interface Callback {
        /**
         * DetailFragmentCallback for when an item has been selected.
         */
        public void onViewCamera();
    }

    public MainFragment() {
        // Required empty public constructor
    }

//    /**
//     * Use this factory method to create a new instance of
//     * this fragment using the provided parameters.
//     *
//     * @param param1 Parameter 1.
//     * @param param2 Parameter 2.
//     * @return A new instance of fragment MainFragment.
//     */
//    // TODO: Rename and change types and number of parameters
//    public static MainFragment newInstance(String param1, String param2) {
//        MainFragment fragment = new MainFragment();
//        Bundle args = new Bundle();
//        args.putString( ARG_PARAM1, param1 );
//        args.putString( ARG_PARAM2, param2 );
//        fragment.setArguments( args );
//        return fragment;
//    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );
        //Add the name of the current Menu
        String title = "Inspección de Lápices";
        getActivity().setTitle(title);

        Log.v(LOG_TAG, ">> onCreate << ");
    }

    @Override
    public void onResume() {
        super.onResume();
        Log.v(LOG_TAG, ">> onResume << ");


        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        Boolean enableCheck = prefs.getBoolean("eCheck", false);
        if (enableCheck) {
            mTextViewCheck.setVisibility( View.VISIBLE );
            mTextViewCheckX.setVisibility( View.GONE );
            mCameraButton.setEnabled(true);
        } else {
            mTextViewCheck.setVisibility( View.GONE );
            mTextViewCheckX.setVisibility( View.VISIBLE );
            mCameraButton.setEnabled(false);
        }

        int refWidth = prefs.getInt("refWidth", 0);
        int refHeight = prefs.getInt("refHeight", 0);

        Log.v(LOG_TAG, ">> refWidth << " + Integer.toString(refWidth) );
        Log.v(LOG_TAG, ">> refHeight << " + Integer.toString(refHeight) );
    }

    @Override
    public void onPause() {
        super.onPause();
        Log.v(LOG_TAG, ">> onPause << ");
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        mReferenceImageButton = (Button) rootView.findViewById(R.id.main_cameraRef_button);
        mReferenceImageButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt("mCameraState", 1); //Camera State
                editor.apply();

                ((Callback) getActivity()).onViewCamera();
            }
        });

        mCameraButton = (Button) rootView.findViewById(R.id.main_camera_button);
        mCameraButton.setOnClickListener( new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
                SharedPreferences.Editor editor = prefs.edit();
                editor.putInt("mCameraState", 2); //Camera State
                editor.apply();

                ((Callback) getActivity()).onViewCamera();
            }
        });

        Log.v(LOG_TAG, ">> onCreateView << ");

        mTextViewCheck = (TextView) rootView.findViewById(R.id.main_text_check);
        mTextViewCheckX = (TextView) rootView.findViewById(R.id.main_text_checkX);

        // Inflate the layout for this fragment
        return rootView;
    }
//
//    // TODO: Rename method, update argument and hook method into UI event
//    public void onButtonPressed(Uri uri) {
//        if (mListener != null) {
//            mListener.onFragmentInteraction( uri );
//        }
//    }

//    @Override
//    public void onAttach(Context context) {
//        super.onAttach( context );
//        if (context instanceof OnFragmentInteractionListener) {
//            mListener = (OnFragmentInteractionListener) context;
//        } else {
//            throw new RuntimeException( context.toString()
//                    + " must implement OnFragmentInteractionListener" );
//        }
//    }
//
//    @Override
//    public void onDetach() {
//        super.onDetach();
//        mListener = null;
//    }
//
//    /**
//     * This interface must be implemented by activities that contain this
//     * fragment to allow an interaction in this fragment to be communicated
//     * to the activity and potentially other fragments contained in that
//     * activity.
//     * <p>
//     * See the Android Training lesson <a href=
//     * "http://developer.android.com/training/basics/fragments/communicating.html"
//     * >Communicating with Other Fragments</a> for more information.
//     */
//    public interface OnFragmentInteractionListener {
//        // TODO: Update argument type and name
//        void onFragmentInteraction(Uri uri);
//    }
}

