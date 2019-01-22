package com.tact.expressway.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import com.google.gson.Gson;
import com.tact.expressway.R;
import com.tact.expressway.adapter.SpinnerAdapter;
import com.tact.expressway.model.EntryExitListModel;
import com.tact.expressway.model.GetEntryExitListSubModel;
import com.tact.expressway.model.VechileClassModel;
import com.tact.expressway.network.ApiClient;
import com.tact.expressway.network.ApiClientInterface;
import com.tact.expressway.utils.Constant;
import com.tact.expressway.utils.SharedPreference;
import com.tact.expressway.utils.Support;
import com.trend.progress.ProgressDialog;

import org.json.JSONObject;

import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TollInformationActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemSelectedListener {
    ApiClientInterface apiClientInterface;
    ProgressDialog progressBar;
    private Spinner spVechileClass;
    private Button btSearch;
    private Spinner spVechileSubClass;
    private Spinner spEntryPoint;
    private Spinner spExitPoint;
    private String journeyId1;
    private String journeyId2;
    private int vechileClassId;
    private int vechileSubClassId;
    private int fairId = 1;
    private ScrollView scrollView;
    private ListView lvTollInformation;
    private String entry_code;
    private String exit_code;
    private String vechileClassName;
    private String vechileSubClassName;
    private String entry_point;
    private String exit_point;
    private String fairType = "Single";
    private SharedPreference sp;

    private boolean journeyId_1 = false;
    private boolean journeyId_2 = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.toll_information);

        sp = new SharedPreference(this);
        apiClientInterface = ApiClient.getApiClient().create(ApiClientInterface.class);
        progressBar = new ProgressDialog(TollInformationActivity.this);
        progressBar.setBarColor(getResources().getColor(Constant.DEFAULT_PROGRESS_COLOR));

        initToolBar("Toll Information" + " (₹)");
        findViews();
        getSpinnerDataOnline();
    }

    //initializes the toolbar
    private void initToolBar(String title) {
        Toolbar toolbar = (Toolbar) findViewById(R.id.my_awesome_toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        setTitle(title);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // handle arrow click here
        if (item.getItemId() == android.R.id.home) {
            finish(); // close this activity and return to preview activity (if there is any)
        }
        return super.onOptionsItemSelected(item);
    }


    //intializes the toolbar
    private void findViews() {
        spVechileClass = (Spinner) findViewById(R.id.spVechileClass);
        spVechileSubClass = (Spinner) findViewById(R.id.spVechileSubClass);

        spVechileClass.setOnItemSelectedListener(this);
        spVechileSubClass.setOnItemSelectedListener(this);

        spEntryPoint = (Spinner) findViewById(R.id.spEntryPoint);
        spExitPoint = (Spinner) findViewById(R.id.spExitPoint);

        spEntryPoint.setOnItemSelectedListener(this);
        spExitPoint.setOnItemSelectedListener(this);

        btSearch = (Button) findViewById(R.id.btSearch);
        btSearch.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();

        switch (id) {
            case R.id.btSearch:

                if (Support.isNetworkOnline(this)) {
                    Support.showToast(this, entry_point);

                    Support.showToast(this, exit_point);

                    if (spVechileClass.getSelectedItemId() == 0) {
                        Support.showToast(this, "-- Choose vehicle class --");
                    } else if (spVechileSubClass.getSelectedItemId() == 0) {
                        Support.showToast(this, "-- Choose vehicle subclass --");
                    } else if (entry_point.contains("-- Choose Entry Point --")) {
                        Support.showToast(this, "-- Choose Entry Point --");
                    } else if (exit_point.contains("-- Choose Exit Point --")) {
                        Support.showToast(this, "-- Choose Exit Point --");
                    } else {
                        getTollRatesByJourneyId();
                    }
                } else {
                    Support.showNoInternetAlertDialog(this);
                }

                break;

            default:
                break;
        }
    }


    private void getSpinnerDataOnline() {
        if (Support.isNetworkOnline(TollInformationActivity.this)) {
            getVehicleClassList();
            getEntryList();
        } else {
            Support.showToast(TollInformationActivity.this, getString(R.string.msg_check_internet));
        }
    }

    private void getExitList(final String entryCode) {
        progressBar.show();
        apiClientInterface.getExitList().enqueue(new Callback<EntryExitListModel>() {
            @Override
            public void onResponse(Call<EntryExitListModel> call, Response<EntryExitListModel> response) {
                progressBar.dismiss();

                if (response.isSuccessful()) {
                    if (response.body().getStatusCode() == 200) {
                        try {
                            EntryExitListModel eelm = response.body();
                            List<GetEntryExitListSubModel> eelsm = eelm.getData();
                            int i = 0;
                            for (GetEntryExitListSubModel ele : eelsm) {
                                if (ele.getExitCode().equalsIgnoreCase(entryCode)) {
                                    eelsm.remove(i);
                                    break;
                                }
                                i++;
                            }

                            String d = new Gson().toJson(eelsm);

                            SpinnerAdapter adapterTo = new SpinnerAdapter(TollInformationActivity.this, d, "-- Choose Exit Point --", R.color.black);
                            spExitPoint.setAdapter(adapterTo);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    Support.showToast(TollInformationActivity.this, "Check your network");
                }
            }

            @Override
            public void onFailure(Call<EntryExitListModel> call, Throwable t) {
                Log.d("Error", t.getMessage());
            }
        });
    }

    private void getEntryList() {
        progressBar.show();
        apiClientInterface.getEntryList().enqueue(new Callback<EntryExitListModel>() {
            @Override
            public void onResponse(Call<EntryExitListModel> call, Response<EntryExitListModel> response) {
                progressBar.dismiss();

                if (response.isSuccessful()) {
                    if (response.body().getStatusCode() == 200) {
                        try {
                            Log.d("response 33: ", new Gson().toJson(response.body()));
                            Log.d("Responce0", response.body().getData() + "");
                            Log.d("Responce1", response.body().getData().toString());

                            JSONObject obj = new JSONObject(new Gson().toJson(response.body()));
                            String d = obj.getString("Data");
                            SpinnerAdapter adapterFrom = new SpinnerAdapter(TollInformationActivity.this, d, "-- Choose Entry Point --", R.color.black);

                            //SpinnerAdapter adapterTo = new SpinnerAdapter(TollInformationActivity.this, d, "-- Choose Exit Point --" , R.color.black);

                            spEntryPoint.setAdapter(adapterFrom);
                            // spExitPoint.setAdapter(adapterTo);

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    Support.showToast(TollInformationActivity.this, "Check your network");
                }
            }

            @Override
            public void onFailure(Call<EntryExitListModel> call, Throwable t) {
                Log.d("Error", t.getMessage());
            }
        });
    }


    private void getVehicleClassList() {
        progressBar.show();
        apiClientInterface.GetVehicleClassList().enqueue(new Callback<VechileClassModel>() {
            @Override
            public void onResponse(Call<VechileClassModel> call, Response<VechileClassModel> response) {
                progressBar.dismiss();

                if (response.isSuccessful()) {
                    if (response.body().getStatusCode() == 200) {
                        try {
                            JSONObject obj = new JSONObject(new Gson().toJson(response.body()));
                            String d = obj.getString("Data");
                            SpinnerAdapter adapter = new SpinnerAdapter(TollInformationActivity.this, d, "-- Choose Vehicle Class --", R.color.black);
                            spVechileClass.setAdapter(adapter);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    Support.showToast(TollInformationActivity.this, "Check your network");
                }
            }

            @Override
            public void onFailure(Call<VechileClassModel> call, Throwable t) {
                Log.d("Error", t.getMessage());
            }
        });
    }


    private void getVechileSubClassByClassId(int vehicleClassId) {
        progressBar.show();
        apiClientInterface.getVehicleSubClassByClassId(vehicleClassId).enqueue(new Callback<VechileClassModel>() {
            @Override
            public void onResponse(Call<VechileClassModel> call, Response<VechileClassModel> response) {
                progressBar.dismiss();

                if (response.isSuccessful()) {
                    if (response.body().getStatusCode() == 200) {
                        try {
                            JSONObject obj = new JSONObject(new Gson().toJson(response.body()));
                            String d = obj.getString("Data");
                            SpinnerAdapter adapter = new SpinnerAdapter(TollInformationActivity.this, d, "-- Choose Vehicle SubClass --", R.color.black);
                            spVechileSubClass.setAdapter(adapter);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                } else {
                    Support.showToast(TollInformationActivity.this, "Check your network");
                }
            }

            @Override
            public void onFailure(Call<VechileClassModel> call, Throwable t) {
                Log.d("Error", t.getMessage());
            }
        });
    }


//    private void hitApiForJourneyData()
//    {
//        progressBar.show();
//        apiClientInterface.getJourneyList().enqueue(new Callback<JourneyModel>()
//        {
//            @Override
//            public void onResponse(Call<JourneyModel> call, Response<JourneyModel> response)
//            {
//                progressBar.dismiss();
//                Log.d("AYUSH", response.isSuccessful() + "");
//
//                if (response.isSuccessful())
//                {
//                        if (response.body().getStatusCode() == 200)
//                        {
//                            try
//                            {
//
//                                // List<VechileClassSupport> vcs = response.body().getData();
//
//                                Log.d("response 33: ", new Gson().toJson(response.body()));
//                                Log.d("Responce0", response.body().getData() + "");
//                                Log.d("Responce1", response.body().getData().toString());
//
//                                JSONObject obj = new JSONObject(new Gson().toJson(response.body()));
//                                String d = obj.getString("Data");
//                                SpinnerAdapter adapterFrom = new SpinnerAdapter(TollInformationActivity.this, d, "-- Choose From --" , R.color.black);
//
//                                SpinnerAdapter adapterTo = new SpinnerAdapter(TollInformationActivity.this, d, "-- Choose To --" , R.color.black);
//                                spFrom.setAdapter(adapterFrom);
//                                spTo.setAdapter(adapterTo);
//
//                            }
//                            catch (Exception e)
//                            {
//                                e.printStackTrace();
//                            }
//                        }
//                }
//                else
//                {
//                    Support.showToast(TollInformationActivity.this, "Check your network");
//                }
//            }
//
//            @Override
//            public void onFailure(Call<JourneyModel> call, Throwable t)
//            {
//                Log.d("Error", t.getMessage());
//            }
//        });
//    }


    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//        if (id == 0)
//        {
//            return;
//        }

        Spinner spinner = (Spinner) parent;

        if (spinner.getId() == R.id.spVechileClass) {
            TextView tv = (TextView) view;
            // Support.showToast(SignUpActivity.this, tv.getText().toString() + "------" + (int) id);
            Log.d("Hi", tv.getText().toString());

            vechileClassId = (int) id;
            vechileClassName = tv.getText().toString();
            getVechileSubClassByClassId(vechileClassId);
        } else if (spinner.getId() == R.id.spVechileSubClass) {
            TextView tv = (TextView) view;
            //Support.showToast(SignUpActivity.this, tv.getText().toString() + "------" + (int) id);
            Log.d("Hi", tv.getText().toString());

            vechileSubClassId = (int) id;
            vechileSubClassName = tv.getText().toString();
        } else if (spinner.getId() == R.id.spEntryPoint) {
            TextView tv = (TextView) view;

            // Support.showToast(TollInformationActivity.this, tv.getText().toString() + "ayush" + (int) id);
            Log.d("Hi", tv.getText().toString());

            journeyId_1 = true;
            journeyId1 = String.valueOf((int) id);

            //   Support.showToast(TollInformationActivity.this ,  sp.getValueString("KEY_ENTRY_CODE"));

            entry_code = sp.getValueString("KEY_ENTRY_CODE");
            Log.d("Entry", sp.getValueString("KEY_ENTRY_CODE"));

            entry_point = tv.getText().toString();


            //ay
            getExitList(entry_code);
        } else if (spinner.getId() == R.id.spExitPoint) {
            TextView tv = (TextView) view;
            // Support.showToast(TollInformationActivity.this, tv.getText().toString() + "ayush" + (int) id);
            Log.d("Hi", tv.getText().toString());

            journeyId2 = String.valueOf((int) id);

            journeyId_2 = true;
            //  Support.showToast(TollInformationActivity.this ,  sp.getValueString("KEY_EXIT_CODE"));

            exit_code = sp.getValueString("KEY_EXIT_CODE");

            Log.d("Entry", sp.getValueString("KEY_EXIT_CODE"));

            exit_point = tv.getText().toString();
        }
    }

    @Override
    public void onNothingSelected(AdapterView<?> parent) {

    }

    private void getTollRatesByJourneyId() {
//        apiClientInterface.getTollRatesByJourneyId("AGR" , "SKH",  1, 1 , 1).enqueue(new Callback<TollRatesByJourneyIdModel>()
//        {
//            @Override
//            public void onResponse(Call<TollRatesByJourneyIdModel> call, Response<TollRatesByJourneyIdModel> response)
//            {
//                Log.d("AYUSH", response.isSuccessful() + "");
//            }
//
//            @Override
//            public void onFailure(Call<TollRatesByJourneyIdModel> call, Throwable t)
//            {
//
//            }
//        });


        Intent i = new Intent(TollInformationActivity.this, TollInformationListView.class);
        i.putExtra("KEY_ENTRY_CODE", entry_code);
        i.putExtra("KEY_EXIT_CODE", exit_code);
        i.putExtra("KEY_VECHILE_CLASS_ID", vechileClassId);
        i.putExtra("KEY_VECHILE_SUB_CLASS_ID", vechileSubClassId);
        i.putExtra("KEY_FAIR_ID", fairId);


        i.putExtra("KEY_VECHILE_CLASS_NAME", vechileClassName);
        i.putExtra("KEY_VECHILE_SUB_CLASS_NAME", vechileSubClassName);
        i.putExtra("KEY_VECHILE_ENTRY_POINT", entry_point);
        i.putExtra("KEY_VECHILE_EXIT_POINT", exit_point);
        i.putExtra("KEY_VECHILE_FAIR_TYPE", fairType);

        startActivity(i);

    }


//    //Our method to show list
//    private void showListinSpinner()
//    {
//        //String array to store all the book names
//        String[] items = new String[departmentNoRealmList.size()];
//
//        //Traversing through the whole list to get all the names
//        for(int i=0; i<departmentNoRealmList.size(); i++)
//        {
//            //Storing names to string array
//            items[i] = departmentNoRealmList.get(i).getDepartmentName();
//        }
//
//        //Spinner spinner = (Spinner) findViewById(R.id.spinner1);
//        ArrayAdapter<String> adapter;
//        adapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_list_item_1, items);
//        //setting adapter to spinner
//        spinnerDepartments.setAdapter(adapter);
//        //Creating an array adapter for list view
//
//    }


    public void onRadioButtonClicked(View view) {
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked
        switch (view.getId()) {
            case R.id.radio_single:
                if (checked)

                    fairId = 1;
                fairType = "Single";

                break;
            case R.id.radio_return:
                if (checked)

                    fairId = 2;
                fairType = "Return";

                break;
            case R.id.radio_smart_card:
                if (checked)

                    fairId = 10;
                fairType = "SmartCard";

                break;
        }
    }


}
