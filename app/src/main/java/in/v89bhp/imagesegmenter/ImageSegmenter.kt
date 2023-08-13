package `in`.v89bhp.imagesegmenter

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import `in`.v89bhp.imagesegmenter.ui.progressbars.CircularProgress

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ImageSegmenter(
    modifier: Modifier = Modifier,
    viewModel: ImageSegmenterViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current

    LaunchedEffect(key1 = true) {
        viewModel.initializeImageSegmentationHelper(context)
    }

    Scaffold(
        topBar = {

            TopAppBar(
                title = {
                    Text(text = stringResource(R.string.app_name))
                },
                actions = {

                },
                navigationIcon = {
//                    IconButton(onClick = {
//                        navigateBack()
//                    }) {
//                        Icon(
//                            imageVector = Icons.Default.ArrowBack,
//                            contentDescription = "Back"
//                        )
//                    }
                })

        }) { contentPadding ->
        if (viewModel.isProcessing || viewModel.loadingImage) {
            CircularProgress(text = stringResource(id = if (viewModel.isProcessing) R.string.processing else R.string.loading_image))
        } else {
            Column(
                modifier = modifier
                    .padding(contentPadding)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (viewModel.imageLoaded) {
                    Image(
                        bitmap = viewModel.imageBitmap!!,
                        contentDescription = "Sample image"
                    )

                    Row {
                        Text(text = stringResource(R.string.image_size))
                        Text(text = viewModel.imageSize)
                    }

                    Row {
                        Text(text = stringResource(R.string.configuration))
                        Text(text = viewModel.imageConfiguration)
                    }

                    Row {
                        Text(text = stringResource(R.string.color_space))
                        Text(text = viewModel.colorSpace)
                    }

                    if (!viewModel.backgroundRemoved) {
                        Button(onClick = { viewModel.removeBackground() }) {
                            Text(text = stringResource(id = R.string.remove_background))
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {

                            ChooseImageButton(viewModel = viewModel)
                            Button(onClick = { /*TODO*/ }) {
                                Text(text = stringResource(id = R.string.save))
                            }
                        }
                    }
                } else {
                    ChooseImageButton(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun ChooseImageButton(
    viewModel: ImageSegmenterViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { imageUri ->
            imageUri?.let {
                viewModel.loadImage(context, it)
            }
        }
    Button(
        modifier = modifier,
        onClick = {
            launcher.launch(
                PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    .build()
            )
        }) {
        Text(text = stringResource(id = R.string.choose_image))
    }
}