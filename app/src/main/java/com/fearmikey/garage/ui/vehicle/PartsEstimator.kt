package com.fearmikey.garage.ui.vehicle

import com.fearmikey.garage.data.local.entity.Vehicle
import com.fearmikey.garage.data.local.entity.VehiclePartsInfo
import com.fearmikey.garage.data.local.entity.VehicleSpecs

/**
 * Generates smart estimated baseline specs for common parts & fluids
 * based on a vehicle's year, make, model, and engine/body specifications.
 * Covers all major North American, Asian, and European automotive brands.
 */
object PartsEstimator {

    fun estimateParts(vehicleId: Long, vehicle: Vehicle?, specs: VehicleSpecs?): VehiclePartsInfo {
        val year = vehicle?.year ?: 2020
        val make = vehicle?.make?.lowercase().orEmpty()
        val model = vehicle?.model?.lowercase().orEmpty()
        val trim = vehicle?.trim?.lowercase().orEmpty()
        val displacement = specs?.displacementL?.toDoubleOrNull() ?: 2.5
        val cylinders = specs?.engineCylinders?.filter { it.isDigit() }?.toIntOrNull() ?: 4
        val fuelType = specs?.fuelType?.lowercase().orEmpty()
        val bodyClass = specs?.bodyClass?.lowercase().orEmpty()
        val vehicleType = specs?.vehicleType?.lowercase().orEmpty()

        val isElectric = fuelType.contains("electric") || fuelType.contains("ev") ||
            make.contains("tesla") || make.contains("rivian") || make.contains("lucid") ||
            make.contains("polestar") || model.contains("mach-e") || model.contains("lightning") ||
            model.contains("ioniq") || model.contains("ev6") || model.contains("id.4")

        val isTruckOrSuv = model.contains("tacoma") || model.contains("f-150") || model.contains("f150") ||
            model.contains("silverado") || model.contains("sierra") || model.contains("tundra") ||
            model.contains("ram") || model.contains("rav4") || model.contains("cr-v") ||
            model.contains("explorer") || model.contains("4runner") || model.contains("outback") ||
            model.contains("tahoe") || model.contains("yukon") || model.contains("suburban") ||
            model.contains("escalade") || model.contains("expedition") || model.contains("navigator") ||
            bodyClass.contains("pickup") || bodyClass.contains("sport utility") ||
            vehicleType.contains("truck") || vehicleType.contains("multipurpose")

        if (isElectric) {
            val psi = if (isTruckOrSuv) "42 PSI" else "38 PSI"
            return VehiclePartsInfo(
                vehicleId = vehicleId,
                oilViscosity = "N/A (Electric)",
                oilCapacity = "N/A (Electric)",
                oilFilterPartNumber = null,
                sparkPlugPartNumber = null,
                sparkPlugGap = "N/A (Electric)",
                tireSizeFront = if (model.contains("model y")) "255/45R19" else if (model.contains("model 3")) "235/45R18" else null,
                tireSizeRear = if (model.contains("model y")) "255/45R19" else if (model.contains("model 3")) "235/45R18" else null,
                tirePsiFront = psi,
                tirePsiRear = psi,
                wiperBladeSizeDriver = if (isTruckOrSuv) "24 in" else "26 in",
                wiperBladeSizePassenger = "19 in",
                wiperBladeSizeRear = if (isTruckOrSuv) "14 in" else null,
            )
        }

        // High-precision model & engine specific lookups
        val spec = estimateBrandSpec(make, model, trim, year, cylinders, displacement, fuelType)

        var viscosity = spec?.viscosity
        var capacity = spec?.capacity
        val sparkPlugGap = spec?.sparkPlugGap ?: "0.040 in"
        val oilFilter = spec?.oilFilterPartNumber
        val sparkPlug = spec?.sparkPlugPartNumber
        val tireSizeFront = spec?.tireSizeFront
        val tireSizeRear = spec?.tireSizeRear ?: tireSizeFront
        val driverWiper = spec?.wiperBladeSizeDriver ?: (if (isTruckOrSuv) "22 in" else "24 in")
        val passengerWiper = spec?.wiperBladeSizePassenger ?: (if (isTruckOrSuv) "20 in" else "19 in")
        val rearWiper = spec?.wiperBladeSizeRear ?: (if (isTruckOrSuv && !bodyClass.contains("pickup")) "16 in" else null)

        // Fallbacks based on engine/year attributes
        if (viscosity == null) {
            viscosity = when {
                fuelType.contains("diesel") -> "5W-40"
                (make.contains("toyota") || make.contains("lexus") || make.contains("honda") ||
                    make.contains("acura") || make.contains("mazda") || make.contains("subaru")) && year >= 2018 -> "0W-16"
                year >= 2010 -> "0W-20"
                year >= 2000 -> "5W-30"
                else -> "10W-30"
            }
        }

        if (capacity == null) {
            capacity = when {
                cylinders >= 8 || displacement >= 5.0 -> "8.0 qts"
                cylinders >= 6 || displacement >= 3.0 -> "6.0 qts"
                displacement >= 2.4 -> "5.2 qts"
                cylinders <= 3 -> "3.7 qts"
                else -> "4.4 qts"
            }
        }

        val psi = if (isTruckOrSuv) "35 PSI" else "32 PSI"

        return VehiclePartsInfo(
            vehicleId = vehicleId,
            oilViscosity = viscosity,
            oilCapacity = capacity,
            oilFilterPartNumber = oilFilter,
            sparkPlugPartNumber = sparkPlug,
            sparkPlugGap = sparkPlugGap,
            tireSizeFront = tireSizeFront,
            tireSizeRear = tireSizeRear,
            tirePsiFront = psi,
            tirePsiRear = psi,
            wiperBladeSizeDriver = driverWiper,
            wiperBladeSizePassenger = passengerWiper,
            wiperBladeSizeRear = rearWiper,
        )
    }

    private data class EngineSpec(
        val capacity: String,
        val viscosity: String,
        val sparkPlugGap: String = "0.040 in",
        val oilFilterPartNumber: String? = null,
        val sparkPlugPartNumber: String? = null,
        val tireSizeFront: String? = null,
        val tireSizeRear: String? = null,
        val wiperBladeSizeDriver: String? = null,
        val wiperBladeSizePassenger: String? = null,
        val wiperBladeSizeRear: String? = null,
    )

    private fun estimateBrandSpec(
        make: String,
        model: String,
        trim: String,
        year: Int,
        cylinders: Int,
        displacement: Double,
        fuelType: String,
    ): EngineSpec? {
        return when {
            make.contains("toyota") || make.contains("lexus") ->
                estimateToyotaLexus(model, year, cylinders, displacement)
            make.contains("honda") || make.contains("acura") ->
                estimateHondaAcura(model, trim, year, cylinders, displacement)
            make.contains("ford") || make.contains("lincoln") ->
                estimateFordLincoln(model, year, cylinders, displacement, fuelType)
            make.contains("chevrolet") || make.contains("chevy") || make.contains("gmc") ||
                make.contains("cadillac") || make.contains("buick") ->
                estimateGM(model, cylinders, displacement, fuelType)
            make.contains("ram") || make.contains("jeep") || make.contains("dodge") || make.contains("chrysler") ->
                estimateStellantis(model, cylinders, displacement)
            make.contains("nissan") || make.contains("infiniti") ->
                estimateNissanInfiniti(year, cylinders, displacement)
            make.contains("subaru") ->
                estimateSubaru(model, trim, cylinders, displacement)
            make.contains("hyundai") || make.contains("kia") || make.contains("genesis") ->
                estimateHyundaiKia(cylinders, displacement)
            make.contains("mazda") ->
                estimateMazda(model, displacement)
            make.contains("bmw") || make.contains("mini") ->
                estimateBMW(year, cylinders, displacement)
            make.contains("volkswagen") || make.contains("vw") || make.contains("audi") || make.contains("porsche") ->
                estimateVAG(cylinders, displacement)
            make.contains("mercedes") || make.contains("benz") ->
                estimateMercedes(cylinders, displacement)
            make.contains("volvo") ->
                EngineSpec(capacity = "6.2 qts", viscosity = "0W-20")
            else -> null
        }
    }

    private fun estimateToyotaLexus(
        model: String,
        year: Int,
        cylinders: Int,
        displacement: Double,
    ): EngineSpec {
        if (model.contains("tacoma")) {
            return when {
                (year in 2016..2023) -> {
                    if (cylinders >= 6 || displacement >= 3.0) {
                        EngineSpec(
                            capacity = "6.2 qts",
                            viscosity = "0W-20",
                            sparkPlugGap = "0.043 in",
                            oilFilterPartNumber = "04152-YZZA1",
                            sparkPlugPartNumber = "Denso FK20HR11",
                            tireSizeFront = "265/70R16",
                            wiperBladeSizeDriver = "22 in",
                            wiperBladeSizePassenger = "20 in",
                        )
                    } else {
                        EngineSpec(
                            capacity = "5.9 qts",
                            viscosity = "0W-20",
                            sparkPlugGap = "0.043 in",
                            oilFilterPartNumber = "90915-YZZD3",
                            sparkPlugPartNumber = "Denso SK20HR11",
                            tireSizeFront = "245/75R16",
                            wiperBladeSizeDriver = "22 in",
                            wiperBladeSizePassenger = "20 in",
                        )
                    }
                }
                (year in 2005..2015) -> {
                    if (cylinders >= 6 || displacement >= 3.8) {
                        EngineSpec(
                            capacity = "5.5 qts",
                            viscosity = "5W-30",
                            oilFilterPartNumber = "90915-YZZD3",
                            sparkPlugPartNumber = "Denso K20HR-U11",
                            tireSizeFront = "265/70R16",
                            wiperBladeSizeDriver = "22 in",
                            wiperBladeSizePassenger = "20 in",
                        )
                    } else {
                        EngineSpec(
                            capacity = "5.8 qts",
                            viscosity = "0W-20",
                            oilFilterPartNumber = "90915-YZZD3",
                            tireSizeFront = "245/75R16",
                            wiperBladeSizeDriver = "22 in",
                            wiperBladeSizePassenger = "20 in",
                        )
                    }
                }
                year >= 2024 -> EngineSpec(capacity = "5.6 qts", viscosity = "0W-20", oilFilterPartNumber = "04152-YZZA1")
                else -> EngineSpec(capacity = "5.5 qts", viscosity = "5W-30")
            }
        }
        if (model.contains("tundra")) {
            return if (year >= 2022 || displacement in 3.3..3.6) {
                EngineSpec(
                    capacity = "7.7 qts",
                    viscosity = "0W-20",
                    oilFilterPartNumber = "04152-YZZA1",
                    tireSizeFront = "265/70R18",
                    wiperBladeSizeDriver = "26 in",
                    wiperBladeSizePassenger = "22 in",
                )
            } else {
                EngineSpec(
                    capacity = "7.9 qts",
                    viscosity = "0W-20",
                    oilFilterPartNumber = "04152-YZZA4",
                    tireSizeFront = "275/65R18",
                    wiperBladeSizeDriver = "26 in",
                    wiperBladeSizePassenger = "22 in",
                )
            }
        }
        if (model.contains("4runner") || model.contains("gx")) {
            return if (year >= 2010 || displacement in 3.8..4.2) {
                EngineSpec(
                    capacity = "6.6 qts",
                    viscosity = "0W-20",
                    oilFilterPartNumber = "04152-YZZA5",
                    tireSizeFront = "265/70R17",
                    wiperBladeSizeDriver = "24 in",
                    wiperBladeSizePassenger = "20 in",
                    wiperBladeSizeRear = "12 in",
                )
            } else {
                EngineSpec(
                    capacity = "5.5 qts",
                    viscosity = "5W-30",
                    oilFilterPartNumber = "90915-YZZD3",
                    tireSizeFront = "265/65R17",
                    wiperBladeSizeDriver = "24 in",
                    wiperBladeSizePassenger = "20 in",
                    wiperBladeSizeRear = "12 in",
                )
            }
        }
        if (model.contains("rav4") || model.contains("camry") || model.contains("corolla") || model.contains("es")) {
            val isRav4 = model.contains("rav4")
            return EngineSpec(
                capacity = if (cylinders >= 6 || displacement >= 3.0) "6.4 qts" else "4.8 qts",
                viscosity = if (year >= 2018) "0W-16" else "0W-20",
                oilFilterPartNumber = "04152-YZZA6",
                tireSizeFront = if (isRav4) "225/65R17" else "215/55R17",
                wiperBladeSizeDriver = "26 in",
                wiperBladeSizePassenger = "16 in",
                wiperBladeSizeRear = if (isRav4) "12 in" else null,
            )
        }
        return EngineSpec(
            capacity = if (cylinders >= 6 || displacement >= 3.0) "6.4 qts" else "4.8 qts",
            viscosity = if (year >= 2018) "0W-16" else "0W-20",
            oilFilterPartNumber = "04152-YZZA6",
        )
    }

    private fun estimateHondaAcura(
        model: String,
        trim: String,
        year: Int,
        cylinders: Int,
        displacement: Double,
    ): EngineSpec {
        if (model.contains("tlx")) {
            return when {
                (year in 2015..2020) -> {
                    if (cylinders >= 6 || displacement >= 3.0) {
                        EngineSpec(
                            capacity = "4.5 qts",
                            viscosity = "0W-20",
                            sparkPlugGap = "0.044 in",
                            oilFilterPartNumber = "15400-PLM-A02",
                            sparkPlugPartNumber = "NGK DILZKR7A11G",
                            tireSizeFront = "225/50R18",
                            wiperBladeSizeDriver = "26 in",
                            wiperBladeSizePassenger = "19 in",
                        )
                    } else {
                        EngineSpec(
                            capacity = "4.4 qts",
                            viscosity = "0W-20",
                            sparkPlugGap = "0.044 in",
                            oilFilterPartNumber = "15400-PLM-A02",
                            sparkPlugPartNumber = "NGK DILKAR7G11GS",
                            tireSizeFront = "225/55R17",
                            wiperBladeSizeDriver = "26 in",
                            wiperBladeSizePassenger = "19 in",
                        )
                    }
                }
                year >= 2021 -> {
                    if (cylinders >= 6 || displacement >= 2.8 || trim.contains("type s")) {
                        EngineSpec(
                            capacity = "5.3 qts",
                            viscosity = "0W-20",
                            sparkPlugGap = "0.032 in",
                            oilFilterPartNumber = "15400-PLM-A02",
                            tireSizeFront = "255/35R20",
                            wiperBladeSizeDriver = "26 in",
                            wiperBladeSizePassenger = "19 in",
                        )
                    } else {
                        EngineSpec(
                            capacity = "5.0 qts",
                            viscosity = "0W-20",
                            sparkPlugGap = "0.032 in",
                            oilFilterPartNumber = "15400-PLM-A02",
                            tireSizeFront = "235/50R18",
                            wiperBladeSizeDriver = "26 in",
                            wiperBladeSizePassenger = "19 in",
                        )
                    }
                }
                else -> EngineSpec(capacity = if (cylinders >= 6) "4.5 qts" else "4.4 qts", viscosity = "0W-20", oilFilterPartNumber = "15400-PLM-A02")
            }
        }
        if (model.contains("mdx") || model.contains("rdx") || model.contains("pilot") ||
            model.contains("passport") || model.contains("odyssey") || model.contains("ridgeline")) {
            return if (cylinders >= 6 || displacement >= 3.0) {
                EngineSpec(
                    capacity = if (year >= 2022 && trim.contains("type s")) "5.3 qts" else "4.5 qts",
                    viscosity = "0W-20",
                    oilFilterPartNumber = "15400-PLM-A02",
                    sparkPlugPartNumber = "NGK DILZKR7A11G",
                    sparkPlugGap = "0.044 in",
                    tireSizeFront = "245/60R18",
                    wiperBladeSizeDriver = "26 in",
                    wiperBladeSizePassenger = "20 in",
                    wiperBladeSizeRear = "14 in",
                )
            } else {
                EngineSpec(
                    capacity = "5.0 qts",
                    viscosity = "0W-20",
                    oilFilterPartNumber = "15400-PLM-A02",
                    tireSizeFront = "235/55R19",
                    wiperBladeSizeDriver = "26 in",
                    wiperBladeSizePassenger = "20 in",
                    wiperBladeSizeRear = "14 in",
                )
            }
        }
        if (model.contains("civic") || model.contains("accord") || model.contains("cr-v") || model.contains("ilx")) {
            val isAccord = model.contains("accord")
            val isCrv = model.contains("cr-v")
            return if (displacement >= 2.0 && cylinders == 4) {
                EngineSpec(
                    capacity = if (year >= 2018 && displacement <= 2.1) "5.0 qts" else "4.4 qts",
                    viscosity = "0W-20",
                    oilFilterPartNumber = "15400-PLM-A02",
                    sparkPlugPartNumber = "NGK DILKAR8A8",
                    sparkPlugGap = "0.032 in",
                    tireSizeFront = if (isAccord) "225/50R17" else if (isCrv) "235/65R17" else "215/55R16",
                    wiperBladeSizeDriver = "26 in",
                    wiperBladeSizePassenger = if (isAccord) "19 in" else "18 in",
                    wiperBladeSizeRear = if (isCrv) "12 in" else null,
                )
            } else if (cylinders >= 6 || displacement >= 3.0) {
                EngineSpec(
                    capacity = "4.5 qts",
                    viscosity = "0W-20",
                    oilFilterPartNumber = "15400-PLM-A02",
                    tireSizeFront = "225/50R17",
                    wiperBladeSizeDriver = "26 in",
                    wiperBladeSizePassenger = "19 in",
                )
            } else {
                EngineSpec(
                    capacity = "3.7 qts",
                    viscosity = "0W-20",
                    oilFilterPartNumber = "15400-PLM-A02",
                    sparkPlugPartNumber = "NGK DILKAR8A8",
                    sparkPlugGap = "0.032 in",
                    tireSizeFront = if (isAccord) "225/50R17" else "215/55R16",
                    wiperBladeSizeDriver = "26 in",
                    wiperBladeSizePassenger = "18 in",
                )
            }
        }
        return EngineSpec(
            capacity = if (cylinders >= 6 || displacement >= 3.0) "4.5 qts" else "4.4 qts",
            viscosity = "0W-20",
            oilFilterPartNumber = "15400-PLM-A02",
        )
    }

    private fun estimateFordLincoln(
        model: String,
        year: Int,
        cylinders: Int,
        displacement: Double,
        fuelType: String,
    ): EngineSpec {
        if (fuelType.contains("diesel") || displacement >= 6.5) {
            return EngineSpec(
                capacity = "13.0 qts",
                viscosity = "10W-30",
                oilFilterPartNumber = "Motorcraft FL-2051S",
                tireSizeFront = "275/70R18",
                wiperBladeSizeDriver = "22 in",
                wiperBladeSizePassenger = "22 in",
            )
        }
        if (model.contains("f-150") || model.contains("f150") || model.contains("expedition") || model.contains("navigator")) {
            return if (cylinders >= 8 || displacement >= 4.8) {
                EngineSpec(
                    capacity = "7.7 qts",
                    viscosity = if (year >= 2021) "0W-20" else "5W-20",
                    oilFilterPartNumber = "Motorcraft FL-500S",
                    sparkPlugPartNumber = "Motorcraft SP-589",
                    sparkPlugGap = "0.040 in",
                    tireSizeFront = "275/65R18",
                    wiperBladeSizeDriver = "22 in",
                    wiperBladeSizePassenger = "22 in",
                )
            } else {
                EngineSpec(
                    capacity = "6.0 qts",
                    viscosity = "5W-30",
                    oilFilterPartNumber = "Motorcraft FL-500S",
                    sparkPlugPartNumber = "Motorcraft SP-580",
                    sparkPlugGap = "0.030 in",
                    tireSizeFront = "275/65R18",
                    wiperBladeSizeDriver = "22 in",
                    wiperBladeSizePassenger = "22 in",
                )
            }
        }
        if (model.contains("mustang")) {
            return if (cylinders >= 8 || displacement >= 4.8) {
                EngineSpec(
                    capacity = "8.0 qts",
                    viscosity = "5W-20",
                    oilFilterPartNumber = "Motorcraft FL-500S",
                    sparkPlugPartNumber = "Motorcraft SP-589",
                    sparkPlugGap = "0.040 in",
                    tireSizeFront = "235/50R18",
                    wiperBladeSizeDriver = "22 in",
                    wiperBladeSizePassenger = "20 in",
                )
            } else {
                EngineSpec(
                    capacity = "5.7 qts",
                    viscosity = "5W-30",
                    oilFilterPartNumber = "Motorcraft FL-910S",
                    sparkPlugPartNumber = "Motorcraft SP-537",
                    sparkPlugGap = "0.030 in",
                    tireSizeFront = "235/55R17",
                    wiperBladeSizeDriver = "22 in",
                    wiperBladeSizePassenger = "20 in",
                )
            }
        }
        return if (cylinders >= 6 || displacement >= 2.7) {
            EngineSpec(capacity = "6.0 qts", viscosity = "5W-30", oilFilterPartNumber = "Motorcraft FL-500S")
        } else {
            EngineSpec(capacity = "5.7 qts", viscosity = "5W-30", oilFilterPartNumber = "Motorcraft FL-910S")
        }
    }

    private fun estimateGM(
        model: String,
        cylinders: Int,
        displacement: Double,
        fuelType: String,
    ): EngineSpec {
        if (fuelType.contains("diesel")) {
            return if (displacement >= 6.0) {
                EngineSpec(
                    capacity = "10.0 qts",
                    viscosity = "15W-40",
                    oilFilterPartNumber = "ACDelco PF2232",
                    tireSizeFront = "275/70R18",
                    wiperBladeSizeDriver = "22 in",
                    wiperBladeSizePassenger = "22 in",
                )
            } else {
                EngineSpec(
                    capacity = "7.0 qts",
                    viscosity = "0W-20",
                    oilFilterPartNumber = "ACDelco PF66",
                    tireSizeFront = "275/60R20",
                    wiperBladeSizeDriver = "22 in",
                    wiperBladeSizePassenger = "22 in",
                )
            }
        }
        if (model.contains("silverado") || model.contains("sierra") || model.contains("tahoe") ||
            model.contains("yukon") || model.contains("suburban") || model.contains("escalade")) {
            return if (cylinders >= 8 || displacement >= 4.8) {
                EngineSpec(
                    capacity = "8.0 qts",
                    viscosity = "0W-20",
                    oilFilterPartNumber = "ACDelco PF63",
                    sparkPlugPartNumber = "ACDelco 41-114",
                    sparkPlugGap = "0.040 in",
                    tireSizeFront = "275/60R20",
                    wiperBladeSizeDriver = "22 in",
                    wiperBladeSizePassenger = "22 in",
                )
            } else {
                EngineSpec(
                    capacity = "6.0 qts",
                    viscosity = "0W-20",
                    oilFilterPartNumber = "ACDelco PF64",
                    tireSizeFront = "275/60R20",
                    wiperBladeSizeDriver = "22 in",
                    wiperBladeSizePassenger = "22 in",
                )
            }
        }
        return if (cylinders >= 8 || displacement >= 5.0) {
            EngineSpec(capacity = "8.0 qts", viscosity = "0W-20", oilFilterPartNumber = "ACDelco PF63")
        } else if (cylinders >= 6 || displacement >= 3.0) {
            EngineSpec(capacity = "6.0 qts", viscosity = "5W-30", oilFilterPartNumber = "ACDelco PF63")
        } else {
            EngineSpec(capacity = "5.0 qts", viscosity = "0W-20", oilFilterPartNumber = "ACDelco PF64")
        }
    }

    private fun estimateStellantis(
        model: String,
        cylinders: Int,
        displacement: Double,
    ): EngineSpec {
        if (model.contains("ram") || model.contains("wrangler") || model.contains("cherokee")) {
            return if (cylinders >= 8 || displacement >= 5.0) {
                EngineSpec(
                    capacity = "7.0 qts",
                    viscosity = "5W-20",
                    oilFilterPartNumber = "Mopar MO-349",
                    sparkPlugPartNumber = "NGK LZFR5C-11",
                    sparkPlugGap = "0.043 in",
                    tireSizeFront = "275/55R20",
                    wiperBladeSizeDriver = "22 in",
                    wiperBladeSizePassenger = "22 in",
                )
            } else if (displacement in 2.9..3.1) {
                EngineSpec(
                    capacity = "7.5 qts",
                    viscosity = "0W-20",
                    oilFilterPartNumber = "Mopar MO-349",
                    tireSizeFront = "275/55R20",
                    wiperBladeSizeDriver = "22 in",
                    wiperBladeSizePassenger = "22 in",
                )
            } else {
                EngineSpec(
                    capacity = "5.0 qts",
                    viscosity = "0W-20",
                    oilFilterPartNumber = "Mopar MO-349",
                    sparkPlugGap = "0.043 in",
                    tireSizeFront = "245/75R17",
                    wiperBladeSizeDriver = "16 in",
                    wiperBladeSizePassenger = "16 in",
                )
            }
        }
        return if (cylinders >= 8 || displacement >= 5.0) {
            EngineSpec(capacity = "7.0 qts", viscosity = "5W-20", oilFilterPartNumber = "Mopar MO-349")
        } else {
            EngineSpec(capacity = "5.0 qts", viscosity = "0W-20", oilFilterPartNumber = "Mopar MO-349")
        }
    }

    private fun estimateNissanInfiniti(
        year: Int,
        cylinders: Int,
        displacement: Double,
    ): EngineSpec {
        return if (cylinders >= 8 || displacement >= 5.0) {
            EngineSpec(capacity = "6.8 qts", viscosity = "0W-20", oilFilterPartNumber = "15208-9E01A")
        } else if (cylinders >= 6 || displacement >= 3.0) {
            EngineSpec(
                capacity = if (displacement in 2.9..3.1) "5.7 qts" else "5.1 qts",
                viscosity = if (year >= 2020) "0W-20" else "5W-30",
                oilFilterPartNumber = "15208-65F0E",
            )
        } else {
            EngineSpec(capacity = "4.8 qts", viscosity = "0W-20", oilFilterPartNumber = "15208-65F0E")
        }
    }

    private fun estimateSubaru(
        model: String,
        trim: String,
        cylinders: Int,
        displacement: Double,
    ): EngineSpec {
        return if (cylinders >= 6 || displacement >= 3.5) {
            EngineSpec(
                capacity = "6.9 qts",
                viscosity = "5W-30",
                oilFilterPartNumber = "15208AA100",
                tireSizeFront = "225/60R18",
                wiperBladeSizeDriver = "26 in",
                wiperBladeSizePassenger = "18 in",
                wiperBladeSizeRear = "14 in",
            )
        } else if (displacement in 2.3..2.4 && (trim.contains("xt") || model.contains("wrx"))) {
            EngineSpec(
                capacity = "4.8 qts",
                viscosity = "0W-20",
                oilFilterPartNumber = "15208AA170",
                sparkPlugPartNumber = "NGK ILKAR8H6",
                sparkPlugGap = "0.024 in",
                tireSizeFront = "225/55R18",
                wiperBladeSizeDriver = "26 in",
                wiperBladeSizePassenger = "18 in",
                wiperBladeSizeRear = "14 in",
            )
        } else {
            EngineSpec(
                capacity = "4.4 qts",
                viscosity = "0W-20",
                oilFilterPartNumber = "15208AA15A",
                sparkPlugPartNumber = "NGK SILKAR7B11",
                sparkPlugGap = "0.043 in",
                tireSizeFront = "225/65R17",
                wiperBladeSizeDriver = "26 in",
                wiperBladeSizePassenger = "18 in",
                wiperBladeSizeRear = "14 in",
            )
        }
    }

    private fun estimateHyundaiKia(
        cylinders: Int,
        displacement: Double,
    ): EngineSpec {
        return if (cylinders >= 6 || displacement >= 3.0) {
            EngineSpec(capacity = if (displacement >= 3.4) "6.9 qts" else "6.0 qts", viscosity = "0W-20", oilFilterPartNumber = "26320-3CKB0")
        } else if (displacement >= 2.4) {
            EngineSpec(capacity = "5.3 qts", viscosity = "0W-20", oilFilterPartNumber = "26350-2S000")
        } else {
            EngineSpec(capacity = "4.2 qts", viscosity = "0W-20", oilFilterPartNumber = "26300-35505")
        }
    }

    private fun estimateMazda(
        model: String,
        displacement: Double,
    ): EngineSpec {
        return if (model.contains("cx-90") || displacement >= 3.0) {
            EngineSpec(capacity = "6.3 qts", viscosity = "0W-20")
        } else if (displacement in 2.4..2.6) {
            EngineSpec(
                capacity = "4.8 qts",
                viscosity = "0W-20",
                oilFilterPartNumber = "1WPE-14-302",
                tireSizeFront = "225/65R17",
                wiperBladeSizeDriver = "24 in",
                wiperBladeSizePassenger = "18 in",
                wiperBladeSizeRear = "14 in",
            )
        } else {
            EngineSpec(capacity = "4.4 qts", viscosity = "0W-20", oilFilterPartNumber = "1WPE-14-302")
        }
    }

    private fun estimateBMW(
        year: Int,
        cylinders: Int,
        displacement: Double,
    ): EngineSpec {
        return if (cylinders >= 8 || displacement >= 4.0) {
            EngineSpec(capacity = "9.0 qts", viscosity = "0W-30")
        } else if (cylinders >= 6 || displacement >= 2.8) {
            EngineSpec(capacity = "6.9 qts", viscosity = if (year >= 2017) "0W-20" else "5W-30", oilFilterPartNumber = "11-42-8-583-898")
        } else {
            EngineSpec(capacity = if (year >= 2017) "5.6 qts" else "5.3 qts", viscosity = if (year >= 2017) "0W-20" else "5W-30", oilFilterPartNumber = "11-42-8-575-211")
        }
    }

    private fun estimateVAG(
        cylinders: Int,
        displacement: Double,
    ): EngineSpec {
        return if (cylinders >= 6 || displacement >= 2.8) {
            EngineSpec(capacity = "7.2 qts", viscosity = "0W-20", oilFilterPartNumber = "06M-198-405-P")
        } else {
            EngineSpec(capacity = "6.0 qts", viscosity = "0W-20", oilFilterPartNumber = "06L-115-562-B")
        }
    }

    private fun estimateMercedes(
        cylinders: Int,
        displacement: Double,
    ): EngineSpec {
        return if (cylinders >= 6 || displacement >= 2.8) {
            EngineSpec(capacity = "8.5 qts", viscosity = "0W-20")
        } else {
            EngineSpec(capacity = "6.3 qts", viscosity = "0W-20", oilFilterPartNumber = "270-180-01-09")
        }
    }
}
