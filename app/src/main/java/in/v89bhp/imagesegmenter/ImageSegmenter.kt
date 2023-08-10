package `in`.v89bhp.imagesegmenter

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.google.accompanist.permissions.ExperimentalPermissionsApi

@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun ImageSegmenter(
    modifier: Modifier = Modifier,
    viewModel: ImageSegmenterViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val context = LocalContext.current
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
        Column(modifier = modifier.padding(contentPadding),
        horizontalAlignment = Alignment.CenterHorizontally) {
            Image(bitmap = viewModel.getImageBitmap(context), contentDescription = "Sample image")

            Button(onClick = { viewModel.removeBackground() }) {
                Text(text = stringResource(id = R.string.remove_background))
            }

        }
    }
}