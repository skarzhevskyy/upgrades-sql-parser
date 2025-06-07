package com.example.ddl;

public class DialectAdapterPostgreSQL implements DialectAdapter {

    @Override
    public String normalizeCheckConstraintsSQL(String checkConstraintSQL, String tableName, String constraintName) {
        // TODO use http://www.antlr.org/
        String regExValuesInArray = "\\[([\\w\\s'\\.,]+)\\]";
        String regExInArray1 = "=\\s+ANY\\s+\\(ARRAY" + regExValuesInArray + "\\)";
        String regExInArray2 = "=\\s+ANY\\s+\\(\\(ARRAY" + regExValuesInArray + "\\)\\)";

        String regExNoInArray1 = "!=\\s+ALL\\s+\\(ARRAY" + regExValuesInArray + "\\)";
        String regExNoInArray2 = "!=\\s+ALL\\s+\\(\\(ARRAY" + regExValuesInArray + "\\)\\)";

        return checkConstraintSQL
                .replaceAll("CHECK (.*)", "$1") // remove pg_get_constraintdef prefix
                .replace("::text[]", "")
                .replace("::text", "")
                .replace("::character varying", "")
                .replace(" <> ", " != ")
                .replaceAll("\\(\"([\\w\\.]+)\"\\)", "\"$1\"") // support for reserved SQL keywords: ("name") -> "name"
                .replaceAll("\\('([\\w\\.]+)'\\)", "'$1'") // cleanup value in in extra brackets: ('text') -> 'text'
                .replaceAll("\\((\\w+)\\)", "$1") // cleanup name in extra brackets: (col_name) -> col_name
                .replaceAll(regExInArray1, "IN ($1)") // cleanup IN variations: ANY (ARRAY['V1', 'V2']) -> IN ('V')
                .replaceAll(regExInArray2, "IN ($1)") // cleanup IN variations: ANY ((ARRAY['V1', 'V2'])) -> IN ('V')
                .replaceAll(regExNoInArray1, "NOT IN ($1)")
                .replaceAll(regExNoInArray2, "NOT IN ($1)") // cleanup IN variations: != ALL ((ARRAY['V1', 'V2'])) -> IN ('V')
                ;
    }

}
