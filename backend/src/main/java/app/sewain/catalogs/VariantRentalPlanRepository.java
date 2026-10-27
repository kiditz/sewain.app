package app.sewain.catalogs;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

interface VariantRentalPlanRepository extends JpaRepository<VariantRentalPlan, UUID> {

}