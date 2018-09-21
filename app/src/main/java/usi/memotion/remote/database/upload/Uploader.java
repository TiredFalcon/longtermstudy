package usi.memotion.remote.database.upload;

import android.content.ContentValues;
import android.database.Cursor;
import android.util.Log;

import usi.memotion.local.database.db.DailyTables;
import usi.memotion.local.database.db.LocalSQLiteDBHelper;
import usi.memotion.remote.database.controllers.SwitchDriveController;
import usi.memotion.local.database.controllers.LocalStorageController;
import usi.memotion.local.database.tableHandlers.TableHandler;
import usi.memotion.local.database.db.LocalDbUtility;
import usi.memotion.local.database.db.LocalTables;
import usi.memotion.local.database.tables.UploaderUtilityTable;
import usi.memotion.remote.database.RemoteStorageController;
import java.util.*;
import java.text.SimpleDateFormat;
import java.util.Map.Entry;


/**
 * Created by usi on 19/01/17.
 */

public class Uploader implements SwitchDriveController.OnTransferCompleted {
    private RemoteStorageController remoteController;
    private LocalStorageController localController;
    private LocalTables tableToClean;
    private long uploadThreshold;
    private String userId;
    private HashMap<String, TableInfo> map;
    LocalSQLiteDBHelper dbHelper;

    public Uploader(String userId, RemoteStorageController remoteController, LocalStorageController localController, long uploadThreshold) {
        this.remoteController = remoteController;
        ((SwitchDriveController) remoteController).setCallback(this);
        this.localController = localController;
        this.uploadThreshold  = uploadThreshold;
        //the start table to clean
        tableToClean = LocalTables.values()[0];
        //user id is the phone id
        this.userId = userId;

        Cursor c = localController.rawQuery("SELECT * FROM uploader_utility", null);
        c.moveToFirst();
        map = new HashMap<>();
//        Log.d("DATA UPLOAD INIT", c.getString(1) + " " + c.getString(2) + " " + c.getInt(3) + " " + c.getString(4));

    }

    public Uploader(String userId, RemoteStorageController remoteController, LocalStorageController localController, LocalSQLiteDBHelper dbHelper) {
        this.remoteController = remoteController;
        this.localController = localController;
        //the start table to clean
        tableToClean = LocalTables.values()[0];
        //user id is the phone id
        this.userId = userId;
        this.dbHelper = dbHelper;
    }

    /**
     * Upload
     */
    public void upload() {

        //upload only when the uploadThreshold is reached
        Log.d("UPLOADER SIZE", "" + localController.getDbSize());
        if(localController.getDbSize() > uploadThreshold) {
            Log.d("DATA UPLOAD SERVICE", "START CLEANING...");

            //if a new day starts, we need to clean the file part array, so that it restart from 0
            String today = buildDate();
            if(!checkDate(today)) {
                cleanFileParts();
                updateDate(today);
            }

            //number of tables
            int nbTableToClean = LocalTables.values().length;
            int i = 0;
            //current table to clean
            LocalTables currTable;

            //clean all tables
            LocalTables[] a  =LocalTables.values();
            for(LocalTables t: LocalTables.values()) {
                processTable(t);
            }
//            while(i < nbTableToClean) {
//                currTable = LocalTables.values()[i];
//                processTable(currTable);
//                i++;
//            }
        }
    }


    /**
     * Upload function to upload local tables' data to Switch Drive
     * Local Tables: Users, Eda, Acc, Bvp, Temp
     *
     */
    public int dailyUpload(){
        int response = 0;
        //number of tables
        int nbTableToClean = DailyTables.values().length;
        int i = 0;
        Cursor c;
        //current table to clean
        DailyTables currTable;
        String fileName;

        while(i < nbTableToClean) {
            currTable = DailyTables.values()[i];

            //build name of file to upload
            fileName = buildFileName(currTable);

            //get all data currently in the table
            c = getRecords(currTable);

            if (c.getCount() > 0) {
                c.moveToFirst();

                //upload the data to the server
                response = remoteController.upload(fileName, toCSV(c, currTable));

                //if the file was put, delete records and update the arrays
                if (response >= 200 && response <= 207) {
                    response = 200;
                } else {
                    response=404;
                    Log.d("DATA UPLOAD SERVICE", "Owncould's response: " + Integer.toString(response));
                }
            }
            i++;
        }

        return response;
    }

//    private String buildSurveyCSV(List<Survey> surveys) {
//        String csv = "";
//        String csvHeader = "";
//
//        for(String surveyColumn: LocalDbUtility.getTableColumns(LocalTables.TABLE_SURVEY)) {
//            csvHeader += surveyColumn + ", ";
//        }
//
//        String[] colums;
//        for(Entry<SurveyType, TableHandler> entry: surveys.get(0).getSurveys().entrySet()) {
//            colums = entry.getValue().getColumns();
//            for(int i = 3; i < colums.length; i++) {
//                csvHeader += entry.getValue().getTableName() + "_" + colums[i] + ", ";
//            }
//        }
//
//        csvHeader = csvHeader.substring(0, csvHeader.length()-2);
//        csv += csvHeader + "\n";
//
//        Map<SurveyType, TableHandler> childs = new HashMap<>();
//        String record = "";
//        String[] childColumns;
//        for(Survey s: surveys) {
//            childs = s.getSurveys();
//
//            for(String column: s.getColumns()) {
//                record += s.getAttributes().getAsString(column) + ", ";
//            }
//
//            for(Map.Entry<SurveyType, TableHandler> entry: childs.entrySet()) {
//                childColumns = entry.getValue().getColumns();
//                for(int i = 3; i < childColumns.length; i++) {
//                    record += entry.getValue().getAttributes().getAsString(childColumns[i]) + ", ";
//                }
//            }
//
//            record += "\n";
//        }
//
//        csv += record;
//
//        return csv.substring(0, csv.length()-1);
//    }
//
//    private String buildSurveyFileName(SurveyType survey) {
//        String today = buildDate();
//        return userId + "_" + today + "_" + survey.getSurveyName() + "_" + "part" + Integer.toString(getFilePartId(LocalTables.TABLE_SURVEY)) + ".csv";
//    }
//
//    private void processSurveys(TableHandler[] surveys) {
//
//        SurveyType currSurvey = ((Survey) surveys[0]).surveyType;
//        String fileName = buildSurveyFileName(currSurvey);
//        String currCsv = "";
//        Survey currS;
//        List<Survey> gSurveys = new ArrayList<>();
//
//        SurveyAlarms alarm;
//        for(TableHandler s: surveys) {
//            currS = (Survey) s;
//            if(currSurvey != currS.surveyType) {
//                currSurvey = currS.surveyType;
//
//                currCsv = buildSurveyCSV(gSurveys);
//                Log.d("AAA", currCsv);
////                alarm = SurveyAlarmSurvey.getAlarm(currS.id);
//
//                TableInfo info = new TableInfo();
//                info.isSurvey = true;
//                info.survey = currSurvey;
//                info.surveysId = new ArrayList<>();
//                for(Survey ss: gSurveys) {
//                    info.surveysId.add(ss.id);
//                }
//                map.put(fileName, info);
//                remoteController.upload(fileName, currCsv);
//
//                fileName = buildSurveyFileName(currSurvey);
//                gSurveys = new ArrayList<>();
//            }
//
//            gSurveys.add(currS);
//
//        }
//
//        fileName = buildSurveyFileName(currSurvey);
//        currCsv = buildSurveyCSV(gSurveys);
//        Log.d("AAA", currCsv);
//        TableInfo info = new TableInfo();
//        info.isSurvey = true;
//        info.survey = currSurvey;
//        info.surveysId = new ArrayList<>();
//        for(Survey ss: gSurveys) {
//            info.surveysId.add(ss.id);
//        }
//        map.put(fileName, info);
//        remoteController.upload(fileName, currCsv);
//
//        incrementFilePartId(LocalTables.TABLE_SURVEY);
//    }

    private void processTable(LocalTables table) {

        Log.d("DATA UPLOAD SERVICE", "Processing table " + LocalDbUtility.getTableName(table));

        if(table == LocalTables.TABLE_LOCATION || table == LocalTables.TABLE_PHONELOCK || table == LocalTables.TABLE_CALL_LOG || table == LocalTables.TABLE_SMS ||
                table == LocalTables.TABLE_NOTIFICATIONS || table == LocalTables.TABLE_USER) {

                String query = getQuery(table);

                Cursor records = localController.rawQuery(query, null);

                if (records.getCount() > 0) {
                    String fileName = buildFileName(table);
                    int startId;
                    int endId;
                    records.moveToFirst();
                    //the starting index
                    startId = records.getInt(0);
                    records.moveToLast();
                    //the ending index
                    endId = records.getInt(0);
                    records.moveToFirst();
                    TableInfo info = new TableInfo();
                    info.table = table;
                    info.startId = startId;
                    info.endId = endId;
                    map.put(fileName, info);

                    Log.d("DATA UPLOAD SERVICE", "Preparing the data to upload for table " + LocalDbUtility.getTableName(table));


                    remoteController.upload(fileName, toCSV(records, table));

                    Log.d("DATA UPLOAD SERVICE", "Upload request sent for table " + LocalDbUtility.getTableName(table));


                } else {
                    Log.d("DATA UPLOAD SERVICE", "Table is empty, nothing to upload");
                }
                records.close();
        } else if(table == LocalTables.TABLE_ACCELEROMETER){
            String query = getQuery(table);
            query += " LIMIT " + 4000;
            Cursor records = localController.rawQuery(query, null);


            if (records.getCount() > 0) {
                String fileName = buildFileName(table);
                int startId;
                int endId;
                records.moveToFirst();
                //the starting index
                startId = records.getInt(0);
                records.moveToLast();
                //the ending index
                endId = records.getInt(0);
                records.moveToFirst();
                TableInfo info = new TableInfo();
                info.table = table;
                info.startId = startId;
                info.endId = endId;
                map.put(fileName, info);

                Log.d("DATA UPLOAD SERVICE", "Preparing the data to upload for table " + LocalDbUtility.getTableName(table));
                remoteController.upload(fileName, toCSV(records, table));

                Log.d("DATA UPLOAD SERVICE", "Upload request sent for table " + LocalDbUtility.getTableName(table));
            } else {
                Log.d("DATA UPLOAD SERVICE", "Table is empty, nothing to upload");
            }
        }

//        else if(table == LocalTables.TABLE_USER){
//            String query = getQuery(table);
//            Cursor records = localController.rawQuery(query, null);
//
//            if (records.getCount() > 0) {
//                String fileName = buildFileName(table);
//                records.moveToFirst();
//                TableInfo info = new TableInfo();
//                info.table = table;
//                map.put(fileName, info);
//
//                Log.d("DATA UPLOAD SERVICE", "Preparing the data to upload for table " + LocalDbUtility.getTableName(table));
//                remoteController.upload(fileName, toCSV(records, table));
//
//                Log.d("DATA UPLOAD SERVICE", "Upload request sent for table " + LocalDbUtility.getTableName(table));
//
//            }else {
//                Log.d("DATA UPLOAD SERVICE", "Table is empty, nothing to upload");
//            }
//        }
    }


//    private void uploadTable(String fileName, String fileContent) {
//        int response = remoteController.upload(fileName, fileContent);
//
//        //if the file was put, delete records and update the arrays
//        if(response >= 200 && response <= 207) {
//            //delete from the db the records where id > startId and id <= endId
//            removeRecords(table, startId, endId);
//            incrementFilePartId(table);
//            updateRecordId(table, endId);
//        } else {
//            Log.d("DATA UPLOAD SERVICE", "Something went wrong, Owncould's response: " + Integer.toString(response));
//        }
//    }

    private String getQuery(LocalTables table) {
        String[] columns = LocalDbUtility.getTableColumns(table);
        String query = "SELECT * FROM " + LocalDbUtility.getTableName(table) +
                " WHERE " + columns[0] + " > " + Integer.toString(getRecordId(table));

        if(table == LocalTables.TABLE_PAM) {
            query += " AND (" + columns[3] + " = " + 1 + " OR " + columns[5] + " = " + 1 + ")";
        }

        return query;
    }

    private boolean checkDate(String date) {
        Cursor c = localController.rawQuery("SELECT * FROM " + UploaderUtilityTable.TABLE_UPLOADER_UTILITY + " WHERE " + UploaderUtilityTable.KEY_UPLOADER_UTILITY_ID + " = 0", null);
        c.moveToNext();

        String d = c.getString(2);

        return date.equals(d);
    }

    /**
     * Utility function to increment the part id for the given table.
     *
     * @param table
     */
    private void incrementFilePartId(LocalTables table) {
        int part = getFilePartId(table);
        Log.d("DATA UPLOAD SERVICE", "INCREASING PART COUNT " + Integer.toString(part+1));

        ContentValues val = new ContentValues();
        val.put(UploaderUtilityTable.KEY_UPLOADER_UTILITY_FILE_PART, part+1);
        String clause = UploaderUtilityTable.KEY_UPLOADER_UTILITY_TABLE + " = \"" + LocalDbUtility.getTableName(table)  + "\"";

        localController.update(UploaderUtilityTable.TABLE_UPLOADER_UTILITY, val, clause);
    }

    /**
     * Utility function to update the record id in the given table.
     *
     * @param table
     * @param recordId
     */
    private void updateRecordId(LocalTables table, int recordId) {
        String clause = UploaderUtilityTable.KEY_UPLOADER_UTILITY_TABLE + " = \"" + LocalDbUtility.getTableName(table) + "\"";
        ContentValues val = new ContentValues();
        val.put(UploaderUtilityTable.KEY_UPLOADER_UTILITY_RECORD_ID, recordId);

        localController.update(UploaderUtilityTable.TABLE_UPLOADER_UTILITY, val, clause);
    }

    /**
     * Utility function to update the date of all tables.
     *
     * @param date
     */
    private void updateDate(String date) {
        String clause;
        ContentValues val;
        for(int i = 0; i < LocalTables.values().length; i++) {
            val = new ContentValues();
            val.put(UploaderUtilityTable.KEY_UPLOADER_UTILITY_DATE, date);
            clause = UploaderUtilityTable.KEY_UPLOADER_UTILITY_TABLE + " = \"" + LocalDbUtility.getTableName(LocalTables.values()[i]) + "\"";

            localController.update(UploaderUtilityTable.TABLE_UPLOADER_UTILITY, val, clause);
        }
    }

    /**
     * Utility function to get the file part of the given table.
     *
     * @param table
     * @return
     */
    private int getFilePartId(LocalTables table) {
        Cursor c = localController.rawQuery("SELECT * FROM " + UploaderUtilityTable.TABLE_UPLOADER_UTILITY + " WHERE " + UploaderUtilityTable.KEY_UPLOADER_UTILITY_TABLE + " = \"" + LocalDbUtility.getTableName(table) + "\"", null);
        c.moveToNext();
        return c.getInt(4);
    }

    private int getRecordId(LocalTables table) {
        Cursor c = localController.rawQuery("SELECT * FROM " + UploaderUtilityTable.TABLE_UPLOADER_UTILITY + " WHERE " + UploaderUtilityTable.KEY_UPLOADER_UTILITY_TABLE + " = \"" + LocalDbUtility.getTableName(table) + "\"", null);
        c.moveToNext();
        return c.getInt(3);
    }

    /**
     * Utility function to clean the file part of all tables.
     */
    private void cleanFileParts() {
        String clause;
        ContentValues val;
        for(int i = 0; i < LocalTables.values().length; i++) {
            clause = UploaderUtilityTable.KEY_UPLOADER_UTILITY_TABLE + " = \"" + LocalDbUtility.getTableName(LocalTables.values()[i]) + "\"";
            val = new ContentValues();
            val.put(UploaderUtilityTable.KEY_UPLOADER_UTILITY_FILE_PART, 0);
            localController.update(UploaderUtilityTable.TABLE_UPLOADER_UTILITY, val, clause);
        }
    }

    /**
     * Build the query to remove the records from the given table, where primary key id in ]start, end].
     *
     * @param table
     * @param start
     * @param end
     */
    private void removeRecords(LocalTables table, int start, int end) {
        Log.d("UPLOAD DATA SERVICE", "Removing from " + Integer.toString(start) + " to " + Integer.toString(end));

        String clause = LocalDbUtility.getTableColumns(table)[0] + " > " + Integer.toString(start) + " AND " +
                LocalDbUtility.getTableColumns(table)[0] + " <= " + Integer.toString(end);
        localController.delete(LocalDbUtility.getTableName(table), clause);
    }

    /**
     * Build the query to select all records from the given table.
     *
     * @param table
     * @return
     */
    private Cursor getRecords(LocalTables table) {
        String query = "SELECT * FROM " + LocalDbUtility.getTableName(table) +
                " WHERE " + LocalDbUtility.getTableColumns(table)[0] + " > " + Integer.toString(getRecordId(table));
        return localController.rawQuery(query, null);
    }

    /**
     * Build the query to select all records from the given table.
     *
     * @param table
     * @return
     */
    private Cursor getRecords(DailyTables table) {
        String query = "SELECT * FROM " + LocalDbUtility.getDailyTableName(table);
        return localController.rawQuery(query, null);
    }

    /**
     * Build the file name.
     *
     * <subjectid>_<date>_<table>_part<nbPart>.csv
     *
     * @param table
     * @return
     */
    private String buildFileName(LocalTables table) {
        //get current date
        String today = buildDate();
        return userId + "_" + today + "_" + LocalDbUtility.getTableName(table) + "_" + "part" + Integer.toString(getFilePartId(table)) + ".csv";
    }

    /**
     * Build the file name.
     *
     * <subjectid>_<date>_<table>_<username>.csv
     *
     * @param table
     * @return
     */
    private String buildFileName(DailyTables table) {
        //get current date
        String today = buildDate();
        String fileName = userId + "_" + today + "_" + LocalDbUtility.getDailyTableName(table) + ".csv";
        return fileName;
    }


    /**
     * Utility function to get the string representation of the today date.
     *
     * @return
     */
    private String buildDate() {
        Calendar calendar = Calendar.getInstance();
        SimpleDateFormat mdformat = new SimpleDateFormat("MM-dd-yyyy");
        return mdformat.format(calendar.getTime());
    }

    /**
     * Generate the csv data from the given cursor.
     *
     * @param records
     * @param table
     * @return
     */
    private String toCSV(Cursor records, LocalTables table) {
        String csv = "";
        String[] columns = LocalDbUtility.getTableColumns(table);

        for(int i = 0; i < columns.length; i++) {
            csv += columns[i] + ",";
        }

        csv = csv.substring(0, csv.length()-1);
        csv += "\n";

        do {
            for(int i = 0; i < columns.length; i++) {
                csv += records.getString(i) + ",";
            }
            csv = csv.substring(0, csv.length()-1);
            csv += "\n";
        } while(records.moveToNext());
        csv = csv.substring(0, csv.length()-1);
        return csv;
    }

    /**
     * Generate the csv data from the given cursor.
     *
     * @param records
     * @param table
     * @return
     */
    private String toCSV(Cursor records, DailyTables table) {
        String csv = "";
        String[] columns = LocalDbUtility.getDailyTableColumns(table);

        for(int i = 0; i < columns.length; i++) {
            csv += columns[i] + ",";
        }

        csv = csv.substring(0, csv.length()-1);
        csv += "\n";

        do {
            for(int i = 0; i < columns.length; i++) {
                csv += records.getString(i) + ",";
            }
            csv = csv.substring(0, csv.length()-1);
            csv += "\n";
        } while(records.moveToNext());
        csv = csv.substring(0, csv.length()-1);
        return csv;
    }

    @Override
    public void onTransferCompleted(String fileName, int status) {
        TableInfo info = map.get(fileName);
        Log.d("UPLOADER", "Got transfer event for file " + fileName);
        if(info != null && info.isSurvey) {
            if(status >= 200 && status <= 207) {
//                markSurveys(info.surveysId);
            } else {
                Log.d("DATA UPLOAD SERVICE", "Something went wrong, Owncould's response: " + Integer.toString(status));
            }
        } else {
            if(status >= 200 && status <= 207) {
                //delete from the db the records where id > startId and id <= endId
                if(info.table != LocalTables.TABLE_SIMPLE_MOOD) {
                    removeRecords(info.table, info.startId, info.endId);
                }

                incrementFilePartId(info.table);
                updateRecordId(info.table, info.endId);
            } else {
                Log.d("DATA UPLOAD SERVICE", "Something went wrong, Owncould's response: " + Integer.toString(status));
            }
        }


        map.remove(fileName);
    }

    private class TableInfo {
        public boolean isSurvey;
//        public SurveyType survey;
        public LocalTables table;
        public int startId;
        public int endId;
        public List<Long> surveysId;
    }

//    private void markSurveys(List<Long> ids) {
//        Survey s;
//        for(Long id: ids) {
//            s = (Survey) Survey.findByPk(id);
//            s.uploaded = true;
//            s.save();
//        }
//    }
}


