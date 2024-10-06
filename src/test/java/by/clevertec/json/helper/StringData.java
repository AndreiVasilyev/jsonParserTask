package by.clevertec.json.helper;

public class StringData {

    private StringData() {
    }

    public static final String DATETIME_OFFSET_CUSTOM_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.nXXX";
    public static final String TEST_JSON_STRING = """
            {
            "id":"3b049946-ed7e-40ba-a7cb-f3585409da22",
            "firstName":"Reuben", 
            "lastName":"Martin",  
            "dateBirth":"2003-11-03",
            "orders":[
                {
                "id":"956bb29b-8191-4de5-9e8e-8df759525831",
                "products":[
                {
                    "id":"50faf7eb-6792-45a7-a3cd-91bb63de48f6",
                    "name":"Телефон",
                    "price":100.0,
                    "quantitiesInStock":{
                          "50faf7eb-6792-45a7-a3cd-91bb63de48f8":  23443,
                          "50faf7eb-6792-45a7-a3cd-91bb63de48f9":  278655
                    }
                },
                {
                    "id":"6b3a9d70-43e0-4c87-b72d-45fe79ee41c4",
                    "name":"Машина",
                    "price":100.0,                    
                    "quantitiesInStock":{
                          "48faf7eb-6792-45a7-a3cd-91bb63de48f8":  343453,
                          "48faf7eb-6792-45a7-a3cd-91bb63de48f9":  345345344
                    }
                }
                ],
                "createDate":"2023-10-24T17:50:30.5470749+03:00"
                }
            ]                     
            }
            """;
    public static final String TEST_JSON_BAD_STRING = """
            {
            "id":"3b049946-ed7e-40ba-a7cb-f3585409da22",
            "firstName":"Reuben", 
            "lastName":"Martin",  
            "dateBirth":["2003-11-03"                    
            }
            """;
}
