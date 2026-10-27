import type { DurationUnit } from "./product";

export interface RentalPlanTemplate {
  durationUnit: DurationUnit;
  durationValue: number;
  percentage: number;
}

export interface RentalPlanSetting{
    plan: RentalPlanTemplate[]
}