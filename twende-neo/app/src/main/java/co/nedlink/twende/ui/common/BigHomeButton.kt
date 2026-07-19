package co.nedlink.twende.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import co.nedlink.twende.ui.theme.Twende

/**
 * The panic-proof HOME control: a very large bottom-centre pill (~2in wide on a
 * 10" head unit) that always brings the driver back to the main screen. Sized
 * so it can be hit with a glance-free thumb at speed.
 */
@Composable
fun BigHomeButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier
            .height(78.dp)
            .width(300.dp)
            .clip(RoundedCornerShape(39.dp))
            .background(Twende.ButtonBg)
            .border(2.dp, Twende.Cyan, RoundedCornerShape(39.dp))
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
    ) {
        Text("⌂", fontSize = 32.sp, color = Twende.Cyan)
        Spacer(Modifier.width(14.dp))
        Text("HOME", fontSize = 26.sp, fontWeight = FontWeight.Black,
            letterSpacing = 8.sp, color = Twende.Ink)
    }
}
