package tn.esprit.agri.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FarmerSolidarityFundId implements Serializable {

    @Column(name = "farmer_id")
    private String farmerId;

    @Column(name = "fund_id")
    private String solidarityFundId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        FarmerSolidarityFundId that = (FarmerSolidarityFundId) o;
        return Objects.equals(farmerId, that.farmerId) &&
                Objects.equals(solidarityFundId, that.solidarityFundId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(farmerId, solidarityFundId);
    }
}