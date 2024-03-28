package `in`.v89bhp.imagesegmenter

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import `in`.v89bhp.imagesegmenter.helpers.ImageSegmentationHelper
import `in`.v89bhp.imagesegmenter.ui.progressbars.CircularProgress
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ImageSegmenter(
    navigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ImageSegmenterViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scaffoldState = rememberBottomSheetScaffoldState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(key1 = true) {
        viewModel.initializeImageSegmentationHelper(context)
        viewModel.loadModelMetadata(context)
    }

    BottomSheetScaffold(

        scaffoldState = scaffoldState,
//        sheetPeekHeight = 128.dp,
        sheetContent = {
//            Box(
//                Modifier
//                    .fillMaxWidth()
//                    .height(128.dp),
//                contentAlignment = Alignment.Center
//            ) {
//                Text("Swipe up to expand sheet")
//            }
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(64.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.model_metadata),
                    style = MaterialTheme.typography.headlineLarge
                )
                Text(viewModel.modelMetadata)
            }
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
        },
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

            if (viewModel.imageDimensionError) {
                val message = stringResource(id = R.string.image_dimension_error, ImageSegmentationHelper.IMAGE_HEIGHT, ImageSegmentationHelper.IMAGE_WIDTH)
                LaunchedEffect(key1 = snackbarHostState) {
                    snackbarHostState.showSnackbar(message = message)
                }
            }

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
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            Button(onClick = {
                                viewModel.removeBackground()
                            }) {
                                Text(text = stringResource(id = R.string.remove_background))
                            }
                            ChooseImageButton(viewModel = viewModel)
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {

                            ChooseImageButton(viewModel = viewModel)
                            Button(
                                onClick = { viewModel.saveImage(context) },
                                enabled = viewModel.imageSaved.not()
                            ) {
                                Text(text = stringResource(id = R.string.save))
                            }
                        }
                        if (viewModel.imageSaved) {
                            val message = stringResource(id = R.string.file_saved)
                            LaunchedEffect(key1 = snackbarHostState) {
                                snackbarHostState.showSnackbar(message = message)
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
            viewModel.imageDimensionError = false
            launcher.launch(
                PickVisualMediaRequest.Builder()
                    .setMediaType(ActivityResultContracts.PickVisualMedia.ImageOnly)
                    .build()
            )
        }) {
        Text(text = stringResource(id = R.string.choose_image))
    }
}