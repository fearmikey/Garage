package com.fearmikey.garage.ui.util

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.fearmikey.garage.R

/*
 * Display text for the preset schedule catalog (data/schedule/MaintenanceScheduleTemplates and the
 * notes in MaintenanceScheduleRules). The catalog stays English because its task names and notes are
 * copied into the user's custom rules; these map that English text to translations for display.
 * Template task names are translated by taskNameRes() in TaskNameFormatter.kt.
 */

@StringRes
fun templateTitleRes(templateId: String): Int? = when (templateId) {
    "motorcycle_street" -> R.string.tpl_motorcycle_street_title
    "heavy_duty" -> R.string.tpl_heavy_duty_title
    "daily_commuter" -> R.string.tpl_daily_commuter_title
    "hybrid_ev" -> R.string.tpl_hybrid_ev_title
    "classic_high_mileage" -> R.string.tpl_classic_title
    else -> null
}

@StringRes
fun templateDescriptionRes(templateId: String): Int? = when (templateId) {
    "motorcycle_street" -> R.string.tpl_motorcycle_street_desc
    "heavy_duty" -> R.string.tpl_heavy_duty_desc
    "daily_commuter" -> R.string.tpl_daily_commuter_desc
    "hybrid_ev" -> R.string.tpl_hybrid_ev_desc
    "classic_high_mileage" -> R.string.tpl_classic_desc
    else -> null
}

/** Translation for a note from the preset catalog; null for notes the user wrote or edited. */
@StringRes
fun scheduleNoteRes(note: String?): Int? = when (note?.trim()) {
    // Built-in rules (MaintenanceScheduleRules)
    "Generic 4WD/AWD interval; check your owner's manual for the exact figure." -> R.string.rule_note_4wd_interval
    "Chain-drive bikes only; also check slack. Ignore this for belt or shaft drive." -> R.string.rule_note_chain_lube
    "Chain-drive bikes only. Replace chain and both sprockets together." -> R.string.rule_note_chain_sprocket
    "Lube and adjust free play on cable-operated clutches and throttles." -> R.string.rule_note_cables
    "Rear tires typically wear 1.5-2x faster than fronts; check tread and date codes." -> R.string.rule_note_moto_tires
    // Street Motorcycle
    "Use JASO MA/MA2 rated oil for wet-clutch bikes." -> R.string.tpl_note_jaso_oil
    "More often after riding in rain or dirt." -> R.string.tpl_note_chain_rain
    "Check cold pressures and inspect for cupping, cracking, and embedded objects." -> R.string.tpl_note_moto_tire_check
    "Brake fluid is hygroscopic; flush both master cylinders." -> R.string.tpl_note_brake_fluid_moto
    "Replace fork oil; inspect seals for weeping." -> R.string.tpl_note_fork_oil
    "Keep on a tender during winter storage; clean and tighten terminals." -> R.string.tpl_note_battery_tender
    // Heavy Duty & Towing
    "Severe duty interval for frequent towing or heavy loads." -> R.string.tpl_note_severe_oil
    "Recommended for frequent 4WD usage or water crossings." -> R.string.tpl_note_transfer_case
    "Severe duty interval for towing and heavy loads." -> R.string.tpl_note_severe_diff
    "Lube grease fittings, drive shaft U-joints, and suspension joints." -> R.string.tpl_note_chassis_grease
    "Inspect brake pad thickness, rotors, trailer brake controller, and leaf springs." -> R.string.tpl_note_hd_brakes
    // Daily Commuter
    "Standard full synthetic oil change interval." -> R.string.tpl_note_synthetic_oil
    "Ensure even tread wear and optimal fuel efficiency." -> R.string.tpl_note_rotation_even
    "Replace both air filters annually for HVAC & engine efficiency." -> R.string.tpl_note_air_filters
    "Replace wiper blades for clear visibility." -> R.string.tpl_note_wipers
    "Replace drive belts to prevent accessory failures." -> R.string.tpl_note_drive_belts
    "Check hoses, lights, and fluid levels." -> R.string.tpl_note_multipoint
    // Hybrid & EV
    "Clean/replace cabin air filter and check high-voltage battery cooling fan intake." -> R.string.tpl_note_hv_duct
    "Regenerative braking reduces pad wear, but slide pins need regular lubrication to prevent seizing." -> R.string.tpl_note_brake_slides
    "Instant EV motor torque causes faster tire wear." -> R.string.tpl_note_ev_tires
    "Inspect and flush high-voltage component cooling system." -> R.string.tpl_note_inverter_coolant
    // High Mileage & Classic
    "Protects flat-tappet camshafts or high-mileage engine seals." -> R.string.tpl_note_high_zinc
    "Inspect radiator hoses, thermostat, heater core, and coolant condition." -> R.string.tpl_note_cooling_hoses
    "Inspect spark plugs, ignition wires, distributor cap, and rotor/points." -> R.string.tpl_note_ignition
    "Check drive belt tension and inspect for cracking or glazing." -> R.string.tpl_note_v_belts
    "Grease tie rod ends, ball joints, pitman arm, and suspension bushings." -> R.string.tpl_note_steering_grease
    else -> null
}

@Composable
fun scheduleNote(note: String): String = scheduleNoteRes(note)?.let { stringResource(it) } ?: note
