# AI Portrait
AI Portrait is an Android app that uses on-device machine learning to remove background from portrait images. It also employs generative AI to upscale images.


- Trained models are deployed on the user's device.
- The app is standalone and makes inferences on the user's device. No data gets exchanged with a backend server.

## Frameworks
- TensorFlow (Version: 2.15.0)
- TensorFlow Lite

## Supported operations

### Remove Background
- Architecture: [U2-Net](https://arxiv.org/abs/2005.09007)
- Dataset: [P3M-10k](https://paperswithcode.com/dataset/p3m-10k)
- Implementation: [GitHub](https://github.com/VaishakNair/PortraitBackgroundRemoverU2Net)

#### Input
![Remove background input](https://i.postimg.cc/wjKgxSCS/Background-Remover-Input.png)

#### Output
![Remove background output](https://i.postimg.cc/Pqyj4NCj/Background-Remover-Output.png)

### Upscale
- Architecture: [SRGAN](https://arxiv.org/abs/1609.04802)
- Datset: [DIV2K](https://data.vision.ee.ethz.ch/cvl/DIV2K/)

#### Input
![Upscale input](https://i.postimg.cc/6pdw8MwT/Upscale-Input.png)

#### Output
![Upscale output](https://i.postimg.cc/0QNqHxSJ/Upscale-Output.png)

##  Android app on Google Play
[![Download](https://play.google.com/intl/en_us/badges/static/images/badges/en_badge_web_generic.png)](https://play.google.com/store/apps/details?id=in.v89bhp.aiportrait)