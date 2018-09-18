package usi.memotion.UI.fragments;


import android.database.Cursor;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import usi.memotion.R;
import usi.memotion.local.database.controllers.LocalStorageController;
import usi.memotion.local.database.controllers.SQLiteController;
import usi.memotion.local.database.db.LocalSQLiteDBHelper;
import usi.memotion.local.database.db.LocalTables;
import usi.memotion.local.database.tables.UserTable;

/**
 * A simple {@link Fragment} subclass.
 */
public class ProfileFragment extends Fragment {

    TextView username, empaticaId, age, gender, status, email, switchtoken, switchpassword;

    LocalSQLiteDBHelper dbHelper;


    public ProfileFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View rootview = inflater.inflate(R.layout.fragment_profile, container, false);
        dbHelper = new LocalSQLiteDBHelper(getContext());

//        User user = dbHelper.getUserInfo(androidId);
//
        username = (TextView) rootview.findViewById(R.id.usernameValue);
        empaticaId = (TextView)rootview.findViewById(R.id.empaticaID);
        email = (TextView)rootview.findViewById(R.id.email);
        switchtoken = (TextView)rootview.findViewById(R.id.switchtoken);
        switchpassword = (TextView)rootview.findViewById(R.id.switchpwd);
        age = (TextView)rootview.findViewById(R.id.ageValue);
        gender = (TextView)rootview.findViewById(R.id.genderValue);
        status = (TextView) rootview.findViewById(R.id.statusValue);

        String query = "SELECT * FROM usersTable";

        LocalStorageController localController = SQLiteController.getInstance(getContext());;

        Cursor records = localController.rawQuery(query, null);
        records.moveToFirst();

        username.setText(records.getString(records.getColumnIndex(UserTable.UserEntry.USERNAME)));
        empaticaId.setText(records.getString(records.getColumnIndex(UserTable.UserEntry.EMPATICAID)));
        switchtoken.setText(records.getString(records.getColumnIndex(UserTable.UserEntry.SWITCH_TOKEN)));
        switchpassword.setText(records.getString(records.getColumnIndex(UserTable.UserEntry.SWITCH_PASSWORD)));
        email.setText(records.getString(records.getColumnIndex(UserTable.UserEntry.EMAIL)));
        age.setText(records.getString(records.getColumnIndex(UserTable.UserEntry.COLUMN_AGE)));
        gender.setText(records.getString(records.getColumnIndex(UserTable.UserEntry.COLUMN_GENDER)));
        status.setText(records.getString(records.getColumnIndex(UserTable.UserEntry.COLUMN_STATUS)));

        records.close();

        return rootview;
    }

}
