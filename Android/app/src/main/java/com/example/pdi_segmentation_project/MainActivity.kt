package com.example.pdi_segmentation_project

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.example.pdi_segmentation_project.ui.theme.PDI_Segmentation_ProjectTheme

class MainActivity : ComponentActivity() {
    private lateinit var segmenter: Segmenter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        segmenter = Segmenter(this)

        setContent {
            PDI_Segmentation_ProjectTheme {
                SegmentationScreen(segmenter)
            }
        }
    }
}

@Composable
fun SegmentationScreen(segmenter: Segmenter) {
    val context = androidx.compose.ui.platform.LocalContext.current

    var selectedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var resultBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var statusText by remember { mutableStateOf("Nenhuma imagem selecionada") }

    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val bitmap = loadBitmapFromUri(context, uri)
            selectedBitmap = bitmap
            resultBitmap = null
            statusText = "Imagem carregada. Clique em Segmentar."
        } else {
            statusText = "Nenhuma imagem selecionada"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Segmentacao Semantica - Oxford-IIIT Pet",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = { imagePicker.launch("image/*") }) {
            Text("Selecionar imagem")
        }

        Spacer(modifier = Modifier.height(16.dp))

        selectedBitmap?.let { bitmap ->
            Text("Imagem original")
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Imagem selecionada",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                contentScale = ContentScale.Fit
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                selectedBitmap?.let { bitmap ->
                    statusText = "Executando segmentacao..."
                    resultBitmap = segmenter.segment(bitmap)
                    statusText = "Segmentacao concluida."
                }
            },
            enabled = selectedBitmap != null
        ) {
            Text("Segmentar")
        }

        Spacer(modifier = Modifier.height(16.dp))

        resultBitmap?.let { bitmap ->
            Text("Resultado com mascara")
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "Resultado da segmentacao",
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
                contentScale = ContentScale.Fit
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = statusText)
    }
}

fun loadBitmapFromUri(context: android.content.Context, uri: Uri): Bitmap {
    val inputStream = context.contentResolver.openInputStream(uri)
    return BitmapFactory.decodeStream(inputStream)
}