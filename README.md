# AI Portrait
AI Portrait is an Android app that uses on-device machine learning to remove background from portrait images. It also employs generative AI to upscale images.


- Trained models are deployed on the user's device.
- The app is standalone and makes inferences on the user's device. No data gets exchanged with a backend server.

## Supported operations

### Remove Background
- Architecture: [U2-Net](https://arxiv.org/abs/2005.09007)
- Dataset: [P3M-10k](https://paperswithcode.com/dataset/p3m-10k)
- Implementation: [GitHub](https://github.com/VaishakNair/PortraitBackgroundRemoverU2Net)



### Upscale
- Architecture: [SRGAN](https://arxiv.org/abs/1609.04802)
- Datset: [DIV2K](https://data.vision.ee.ethz.ch/cvl/DIV2K/)
