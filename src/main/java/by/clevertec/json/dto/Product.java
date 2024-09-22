package by.clevertec.json.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class Product {
    private UUID id;
    private String name;
    private Double price;
    private Map<UUID, BigDecimal> quantitiesInStock;

}
