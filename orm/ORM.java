package partial_framework.orm;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import flashback.Singleton;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/*
 * First of all you should use (@ORMTable(TableName = "NAME"))
 * annotation before database model class
 *
 * then use (@ORMField(Name = "b", isPrimaryKey = true))
 * annotation for each data field and also initial a default value
 * for the field
 * //***Important to avoid reflection bugs***
 * @ORMField(Name = "b", isPrimaryKey = true)
 * private int id = 0;//Initialization is recommended for ORM ***very important***
 */

//TODO: make default values when creating tables
public class ORM<T> {

    public String getTableName(Class<T> cls) {
        ORMTable an = cls.getAnnotation(ORMTable.class);
        return an.TableName();
    }

    public void save(SQLiteDatabase db, T obj) {
        @SuppressWarnings("unchecked") Class<T> cls = (Class<T>) obj.getClass();

        String fields = "", values = "";
        for (Field f : cls.getDeclaredFields()) {
            f.setAccessible(true);
            ORMField an = f.getAnnotation(ORMField.class);
            if (an != null) {
                String javaType = fixType(f.getGenericType());
                String sqlType = convertJavaTypeToSqlType(javaType);
                //noinspection StringConcatenationInLoop
                fields += an.name() + ",";
                String del = sqlType.equals("text") ? "'" : "";
                String val = "";
                try {
                    val = f.get(obj).toString();
                } catch (Exception e) {
                    e.printStackTrace();
                }
                String v = del + val + del + ",";
                //noinspection StringConcatenationInLoop
                values += v;
            }
            f.setAccessible(false);
        }

        fields += "--)";
        values += "--)";
        fields = fields.replace(",--)", "");
        values = values.replace(",--)", "");

        String str = "insert or replace into " + getTableName(cls) + "(" + fields + ") values (" + values + ")";
        db.execSQL(str);

        /*try {
            db.execSQL(str);
            Util.flatAlert("Done", str);
        } catch (Exception ex) {
            Util.flatAlert("Error", ex.toString());
        }*/
    }

    public ArrayList<T> load(SQLiteDatabase db, T obj, String where) {
        @SuppressWarnings("unchecked") Class<T> cls = (Class<T>) obj.getClass();
        ArrayList<T> list = new ArrayList<>();

        if (where == null) where = "";

        Cursor cursor = db.rawQuery("select * from " + getTableName(cls) + " " + where, null);
        if (cursor == null) return null;
        cursor.moveToFirst();

        Set<Field> set = new HashSet<>();
        for (Field field : cls.getDeclaredFields()) {
            if (field.isAnnotationPresent(ORMField.class)) {
                set.add(field);
            }
        }

        while (!cursor.isAfterLast()) {
            T o = null;
            //noinspection CatchMayIgnoreException
            try {
                o = cls.newInstance();
            } catch (Exception ex) {
            }
            int num = cursor.getColumnCount();
            for (int i = 0; i < num; i++) {
                String colName = cursor.getColumnName(i);
                Field field = null;
                for (Field f : set) {
                    if (f.getAnnotation(ORMField.class).name().equals(colName)) {
                        field = f;
                        break;
                    }
                }
                String type = fixType(Objects.requireNonNull(field).getGenericType());

                //Util.flatAlert("", colName + " - " + field.getGenericType() + " - " + type);

                try {
                    field.setAccessible(true);
                    switch (type) {
                        case "string":
                            //FIXME: extend this later for arrays, date and so on
                            field.set(o, cursor.getString(i));
                            break;
                        case "boolean":
                            field.setBoolean(o, cursor.getInt(i) == 1);
                            break;
                        case "float":
                            field.setFloat(o, cursor.getFloat(i));
                            break;
                        case "int":
                            field.setInt(o, cursor.getInt(i));
                            break;
                        case "double":
                            field.setDouble(o, cursor.getDouble(i));
                            break;
                    }
                    field.setAccessible(false);
                } catch (Exception ignored) {
                    Singleton.debugAlert(colName + " - " + field.getGenericType() + " - " + type + "__________" + ignored);
                }
            }

            cursor.moveToNext();
            list.add(o);
        }

        cursor.close();//very important to close cursor

        return list;
    }

    private String fixType(Type t) {
        String type = t.toString();
        type = type.toLowerCase();
        type = type.replace(" ", "");
        type = type.replace("class", "");
        type = type.replace("java.", "");
        type = type.replace("lang.", "");
        type = type.replace("util.", "");
        switch (type) {
            case "[z":
                type = "bool_arr";
                break;
            //noinspection SpellCheckingInspection
            case "[linteger;"://the spelling is true
                type = "int_arr";
                break;
            case "[d":
                type = "double_arr";
                break;
        }
        return type;
    }

    private String convertJavaTypeToSqlType(String fixedType) {
        switch (fixedType) {
            case "string":
                return "text";
            case "int":
                return "integer";
            case "float":
                return "real";
            case "boolean":
                return "integer";//not supporting bool
            case "double":
                return "text";//then fetch it from database with getDouble()
        }

        //noinspection SpellCheckingInspection
        return "text";//bool_arr, int_arr, double_arr, arraylist, date
    }

    public void build(SQLiteDatabase db, T obj) {
        @SuppressWarnings("unchecked") Class<T> cls = (Class<T>) obj.getClass();

        String str = "create table if not exists " + getTableName(cls) + "(";
        for (Field f : cls.getDeclaredFields()) {
            f.setAccessible(true);
            ORMField an = f.getAnnotation(ORMField.class);
            if (an != null) {
                String sqlType = convertJavaTypeToSqlType(fixType(f.getGenericType()));
                String nullable = an.canBeNull() && (!an.isPrimaryKey()) ? " " : " not null ";
                String primary = an.isPrimaryKey() ? " primary key " : " ";
                String unique = (an.isUnique() || an.isPrimaryKey()) ? " unique " : " ";
                String defaultValue = "";//FIXME
                String autoIncrement = "";//FIXME
                //noinspection StringConcatenationInLoop
                str += "\n" + an.name() + " " + sqlType + nullable + unique + primary + ",";
            }
            f.setAccessible(false);
        }

        str = str.replaceAll(" {2}", " ");
        str = str.replaceAll(" ,", ",");
        str = str + "--)";
        str = str.replace(",--)", "\n);");

        try {
            db.execSQL(str);
            //Util.flatAlert("Done", str);
        } catch (Exception ex) {
            //Util.flatAlert("Error", ex.toString());
        }
    }

    public void removeAllRecords(SQLiteDatabase db, T obj) {
        @SuppressWarnings("unchecked") Class<T> cls = (Class<T>) obj.getClass();

        db.execSQL("delete from " + getTableName(cls));
    }
}
