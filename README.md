# MicroAndroidORM
MIT License

    @ORMTable(TableName="TestEntityTable")
    public class TestEntity {
        @ORMField(Name = "ID", isPrimaryKey = true)
        private int id = 0;//Initialization is recommended for ORM ***very important***

        @ORMField(Name="Name",isUnique=true)
        private String name="";

        @ORMField(Name="InsertionDate")
        private String date="";
    }
