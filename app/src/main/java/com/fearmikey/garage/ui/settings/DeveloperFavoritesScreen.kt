package com.fearmikey.garage.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.ElectricBolt
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.Hardware
import androidx.compose.material.icons.filled.VideoCameraFront
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage

data class RecommendedProduct(
    val name: String,
    val description: String,
    val url: String,
    val category: String,
    val icon: ImageVector,
    val imageUrl: String? = null
)

val developerFavorites = listOf(
    // Hand Tools
    RecommendedProduct(
        name = "KNIPEX Pliers Wrench (10-Inch)",
        description = "Adjustable pliers wrench for precise, smooth jaw grips.",
        url = "https://link.amazon/B0ecZNiyV",
        category = "Hand Tools",
        icon = Icons.Default.Handyman
    ),
    RecommendedProduct(
        name = "KNIPEX Cobra Water Pump Pliers (5-Inch)",
        description = "Compact Cobra pliers with self-locking jaw.",
        url = "https://link.amazon/B02hlfOtr",
        category = "Hand Tools",
        icon = Icons.Default.Handyman
    ),
    RecommendedProduct(
        name = "KNIPEX Electronics Super Knips",
        description = "Comfort grip flush cutters for zip ties and electronics.",
        url = "https://link.amazon/B0c3NNAvH",
        category = "Hand Tools",
        icon = Icons.Default.Handyman
    ),
    RecommendedProduct(
        name = "GEARWRENCH 120XP Flex Ratchet Set (4-Piece)",
        description = "1/4\", 3/8\", & 1/2\" drive flex ratchets.",
        url = "https://link.amazon/B05Maf4N5",
        category = "Hand Tools",
        icon = Icons.Default.Handyman
    ),
    RecommendedProduct(
        name = "GEARWRENCH Mechanics Tool Set (56-Piece)",
        description = "3/8\" Drive 6-Point SAE/Metric socket and ratchet set.",
        url = "https://link.amazon/B0j0cnWZ3",
        category = "Hand Tools",
        icon = Icons.Default.Handyman
    ),
    RecommendedProduct(
        name = "GEARWRENCH Impact Socket Set (65-Piece)",
        description = "1/2\" Drive standard and deep SAE/Metric impact sockets.",
        url = "https://link.amazon/B04Bib7U8",
        category = "Hand Tools",
        icon = Icons.Default.Handyman
    ),

    // Power Tools (Milwaukee M18)
    RecommendedProduct(
        name = "Milwaukee M18 FUEL 1/2\" High Torque Impact Wrench",
        description = "High torque impact for stubborn suspension and lug nuts (Tool Only).",
        url = "https://link.amazon/B09W8yDhg",
        category = "Power Tools",
        icon = Icons.Default.Build
    ),
    RecommendedProduct(
        name = "Milwaukee M18 FUEL 3/8\" Compact Impact Wrench",
        description = "Compact impact wrench for tight engine bays (Tool Only).",
        url = "https://link.amazon/B0aKveud2",
        category = "Power Tools",
        icon = Icons.Default.Build
    ),
    RecommendedProduct(
        name = "Milwaukee M18 FUEL 4-1/2\" / 5\" Angle Grinder",
        description = "Brushless angle grinder with paddle switch (Tool Only).",
        url = "https://link.amazon/B06yNsAha",
        category = "Power Tools",
        icon = Icons.Default.Build
    ),
    RecommendedProduct(
        name = "Milwaukee M18 FUEL 1/2\" Hammer Drill/Driver",
        description = "Heavy-duty hammer drill for fabrication.",
        url = "https://link.amazon/B03fKVfXS",
        category = "Power Tools",
        icon = Icons.Default.Build
    ),
    RecommendedProduct(
        name = "Milwaukee M18 FUEL Sawzall Reciprocating Saw",
        description = "Cordless reciprocating saw for demolition (Tool Only).",
        url = "https://link.amazon/B0dQ8nV0T",
        category = "Power Tools",
        icon = Icons.Default.Build
    ),
    RecommendedProduct(
        name = "Milwaukee M18 2.5 Gal Vacuum",
        description = "Wet/dry portable shop vacuum.",
        url = "https://link.amazon/B0dL0tWP6",
        category = "Power Tools",
        icon = Icons.Default.CleaningServices
    ),
    RecommendedProduct(
        name = "Milwaukee M18 XC 5.0 Battery & Charger Kit",
        description = "Includes two 5.0 Ah batteries and M12/M18 charger.",
        url = "https://link.amazon/B00CZIXTt",
        category = "Power Tools",
        icon = Icons.Default.ElectricBolt
    ),
    RecommendedProduct(
        name = "Milwaukee M18 2.0 Compact Battery",
        description = "Lightweight battery pack for smaller tools.",
        url = "https://link.amazon/B01vda0lt",
        category = "Power Tools",
        icon = Icons.Default.ElectricBolt
    ),

    // Cleaning, Chemical & Lube
    RecommendedProduct(
        name = "PB Blaster Penetrating Oil (13.2 oz)",
        description = "Rust remover and penetrant catalyst for stuck bolts.",
        url = "https://link.amazon/B0jaASmAU",
        category = "Chemicals & Fluids",
        icon = Icons.Default.Hardware
    ),
    RecommendedProduct(
        name = "WD-40 Specialist Penetrant",
        description = "Fast-acting rust penetrant with smart straw.",
        url = "https://link.amazon/B0bPRfsGi",
        category = "Chemicals & Fluids",
        icon = Icons.Default.Hardware
    ),
    RecommendedProduct(
        name = "Fluid Film Rust Protection (6-Pack Aerosol)",
        description = "Lanolin-based rust inhibitor and undercoating spray.",
        url = "https://link.amazon/B03H5aEvt",
        category = "Chemicals & Fluids",
        icon = Icons.Default.Hardware
    ),
    RecommendedProduct(
        name = "Fluid Film Rust Protection (1 Gallon)",
        description = "Bulk rust prevention coating for underbodies.",
        url = "https://link.amazon/B02kth0SN",
        category = "Chemicals & Fluids",
        icon = Icons.Default.Hardware
    ),
    RecommendedProduct(
        name = "Adam's Iron Remover (16oz)",
        description = "Fallout and brake dust remover for paint and wheels.",
        url = "https://link.amazon/B0fpsIY36",
        category = "Chemicals & Fluids",
        icon = Icons.Default.CleaningServices
    ),
    RecommendedProduct(
        name = "Chemical Guys Clay Bar & Luber Kit",
        description = "6-piece kit for smooth paint decontamination.",
        url = "https://link.amazon/B0480hkQm",
        category = "Chemicals & Fluids",
        icon = Icons.Default.CleaningServices
    ),
    RecommendedProduct(
        name = "CLR PRO Calcium, Lime & Rust Remover",
        description = "28 oz bottle for rust and mineral deposits.",
        url = "https://link.amazon/B0cykOy45",
        category = "Chemicals & Fluids",
        icon = Icons.Default.CleaningServices
    ),
    RecommendedProduct(
        name = "Scott Shop Towels (3 Rolls)",
        description = "Heavy-duty blue paper towels.",
        url = "https://link.amazon/B05Q0l1w7",
        category = "Chemicals & Fluids",
        icon = Icons.Default.CleaningServices
    ),

    // Electrical & Diagnostics
    RecommendedProduct(
        name = "Fluke 117 Digital Multimeter",
        description = "Reliable DMM with non-contact voltage detection.",
        url = "https://link.amazon/B04gYTXt6",
        category = "Electrical",
        icon = Icons.Default.ElectricBolt
    ),
    RecommendedProduct(
        name = "Blue Sea Systems ST Blade Fuse Block",
        description = "12 circuits with negative bus for 12V accessories.",
        url = "https://link.amazon/B0hslHT3W",
        category = "Electrical",
        icon = Icons.Default.ElectricBolt
    ),
    RecommendedProduct(
        name = "BESTEK 500W Pure Sine Wave Power Inverter",
        description = "12V DC to 110V AC inverter with 2 AC and 2 USB ports.",
        url = "https://link.amazon/B0h8UMA8H",
        category = "Electrical",
        icon = Icons.Default.ElectricBolt
    ),

    // Overlanding & Accessories
    RecommendedProduct(
        name = "RotopaX 2 Gallon Water Pack",
        description = "White, leakproof portable water container.",
        url = "https://link.amazon/B0ghukZFz",
        category = "Off-Road & Accessories",
        icon = Icons.Default.Build
    ),
    RecommendedProduct(
        name = "RotopaX 2 Gallon Gasoline Pack",
        description = "Red, leakproof portable fuel container.",
        url = "https://link.amazon/B06Qy4unM",
        category = "Off-Road & Accessories",
        icon = Icons.Default.Build
    ),
    RecommendedProduct(
        name = "RotopaX DLX Pack Mount",
        description = "Mounting hardware for RotopaX containers.",
        url = "https://link.amazon/B09yzpsRP",
        category = "Off-Road & Accessories",
        icon = Icons.Default.Build
    ),
    RecommendedProduct(
        name = "Original Quick Fist Clamp (2-Pack)",
        description = "Mount tools 1\" to 2-1/4\" in diameter.",
        url = "https://link.amazon/B03YgIXYa",
        category = "Off-Road & Accessories",
        icon = Icons.Default.Build
    ),
    RecommendedProduct(
        name = "First Alert Fire Extinguisher",
        description = "Rated for automotive and marine use.",
        url = "https://link.amazon/B03YqTHR5",
        category = "Off-Road & Accessories",
        icon = Icons.Default.Build
    ),
    RecommendedProduct(
        name = "VIOFO A119 V3 Dash Cam",
        description = "2K front recording with GPS and HDR.",
        url = "https://link.amazon/B0cM7SbaA",
        category = "Off-Road & Accessories",
        icon = Icons.Default.VideoCameraFront
    ),
    RecommendedProduct(
        name = "80 mil Butyl Sound Deadening Mat",
        description = "36 sqft audio noise insulation and dampening.",
        url = "https://link.amazon/B0ehtyC73",
        category = "Off-Road & Accessories",
        icon = Icons.Default.Build
    )
)

@Composable
fun DeveloperFavoritesContent() {
    val uriHandler = LocalUriHandler.current

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Tools and parts recommended by the developer (FearMikey). Purchasing through these Amazon affiliate links helps support the project at no extra cost to you. Usually these links earn FearMikey 2-5% of your Amazon purchase. Thank you!",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        // Group by category
        val groupedFavorites = developerFavorites.groupBy { it.category }

        groupedFavorites.forEach { (category, products) ->
            item {
                Text(
                    text = category,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }

            items(products) { product ->
                ProductCard(
                    product = product
                ) { uriHandler.openUri(product.url) }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun ProductCard(
    product: RecommendedProduct,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (product.imageUrl != null) {
                AsyncImage(
                    model = product.imageUrl,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .size(48.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Icon(
                    imageVector = product.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(end = 16.dp)
                )
            }
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = product.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Icon(
                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                contentDescription = "Open Link",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}
