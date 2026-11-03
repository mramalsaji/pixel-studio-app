package com.example.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.RectF
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.ProcessedImageEntity
import com.example.data.repository.ImageRepository
import com.example.domain.model.*
import com.example.domain.processing.ExifHelper
import com.example.domain.processing.ImageProcessor
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.UUID

enum class AppScreen {
    HOME,
    SINGLE_EDITOR,
    BATCH_PROCESS,
    GALLERY
}

enum class EditorTab {
    CROP,
    RESIZE,
    FORMAT,
    EXIF
}

class EditorViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ImageRepository(application)

    // Navigation
    private val _currentScreen = MutableStateFlow(AppScreen.HOME)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    // Single Editor State
    private val _singleImageUri = MutableStateFlow<Uri?>(null)
    val singleImageUri: StateFlow<Uri?> = _singleImageUri.asStateFlow()

    private val _singleBitmap = MutableStateFlow<Bitmap?>(null)
    val singleBitmap: StateFlow<Bitmap?> = _singleBitmap.asStateFlow()

    private val _singleFileName = MutableStateFlow("")
    val singleFileName: StateFlow<String> = _singleFileName.asStateFlow()

    private val _singleFileSize = MutableStateFlow(0L)
    val singleFileSize: StateFlow<Long> = _singleFileSize.asStateFlow()

    private val _originalDimensions = MutableStateFlow(Pair(0, 0))
    val originalDimensions: StateFlow<Pair<Int, Int>> = _originalDimensions.asStateFlow()

    private val _cropRatio = MutableStateFlow(CropRatio.FREE)
    val cropRatio: StateFlow<CropRatio> = _cropRatio.asStateFlow()

    private val _cropRect = MutableStateFlow(RectF(0f, 0f, 1f, 1f))
    val cropRect: StateFlow<RectF> = _cropRect.asStateFlow()

    private val _rotationDegrees = MutableStateFlow(0f)
    val rotationDegrees: StateFlow<Float> = _rotationDegrees.asStateFlow()

    private val _flipHorizontal = MutableStateFlow(false)
    val flipHorizontal: StateFlow<Boolean> = _flipHorizontal.asStateFlow()

    private val _flipVertical = MutableStateFlow(false)
    val flipVertical: StateFlow<Boolean> = _flipVertical.asStateFlow()

    private val _resizeConfig = MutableStateFlow(ResizeConfig())
    val resizeConfig: StateFlow<ResizeConfig> = _resizeConfig.asStateFlow()

    private val _selectedFormat = MutableStateFlow(ImageFormat.JPEG)
    val selectedFormat: StateFlow<ImageFormat> = _selectedFormat.asStateFlow()

    private val _quality = MutableStateFlow(90)
    val quality: StateFlow<Int> = _quality.asStateFlow()

    private val _stripMetadata = MutableStateFlow(false)
    val stripMetadata: StateFlow<Boolean> = _stripMetadata.asStateFlow()

    private val _exifData = MutableStateFlow(ExifData())
    val exifData: StateFlow<ExifData> = _exifData.asStateFlow()

    private val _activeEditorTab = MutableStateFlow(EditorTab.CROP)
    val activeEditorTab: StateFlow<EditorTab> = _activeEditorTab.asStateFlow()

    private val _isSingleProcessing = MutableStateFlow(false)
    val isSingleProcessing: StateFlow<Boolean> = _isSingleProcessing.asStateFlow()

    private val _estimatedOutputSizeBytes = MutableStateFlow<Long?>(null)
    val estimatedOutputSizeBytes: StateFlow<Long?> = _estimatedOutputSizeBytes.asStateFlow()

    private val _isEstimatingSize = MutableStateFlow(false)
    val isEstimatingSize: StateFlow<Boolean> = _isEstimatingSize.asStateFlow()

    private var estimateJob: Job? = null

    // Batch State
    private val _batchItems = MutableStateFlow<List<BatchItem>>(emptyList())
    val batchItems: StateFlow<List<BatchItem>> = _batchItems.asStateFlow()

    private val _isBatchProcessing = MutableStateFlow(false)
    val isBatchProcessing: StateFlow<Boolean> = _isBatchProcessing.asStateFlow()

    private val _showBatchProgressDialog = MutableStateFlow(false)
    val showBatchProgressDialog: StateFlow<Boolean> = _showBatchProgressDialog.asStateFlow()

    private val _batchFormat = MutableStateFlow(ImageFormat.JPEG)
    val batchFormat: StateFlow<ImageFormat> = _batchFormat.asStateFlow()

    private val _batchQuality = MutableStateFlow(85)
    val batchQuality: StateFlow<Int> = _batchQuality.asStateFlow()

    private val _batchCropRatio = MutableStateFlow(CropRatio.FREE)
    val batchCropRatio: StateFlow<CropRatio> = _batchCropRatio.asStateFlow()

    private val _batchResizeConfig = MutableStateFlow(ResizeConfig())
    val batchResizeConfig: StateFlow<ResizeConfig> = _batchResizeConfig.asStateFlow()

    private val _batchStripMetadata = MutableStateFlow(false)
    val batchStripMetadata: StateFlow<Boolean> = _batchStripMetadata.asStateFlow()

    private var batchJob: Job? = null

    // Gallery State
    val galleryImages: StateFlow<List<ProcessedImageEntity>> = repository.allProcessedImages
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedGalleryItem = MutableStateFlow<ProcessedImageEntity?>(null)
    val selectedGalleryItem: StateFlow<ProcessedImageEntity?> = _selectedGalleryItem.asStateFlow()

    private val _galleryMultiSelectedIds = MutableStateFlow<Set<Long>>(emptySet())
    val galleryMultiSelectedIds: StateFlow<Set<Long>> = _galleryMultiSelectedIds.asStateFlow()

    private val _galleryFormatFilter = MutableStateFlow<String?>("ALL")
    val galleryFormatFilter: StateFlow<String?> = _galleryFormatFilter.asStateFlow()

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    fun clearToastMessage() {
        _toastMessage.value = null
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun setActiveEditorTab(tab: EditorTab) {
        _activeEditorTab.value = tab
    }

    fun loadSingleImage(uri: Uri) {
        _singleImageUri.value = uri
        _singleBitmap.value = null // clear previous bitmap to prevent memory leak
        _isSingleProcessing.value = true
        _cropRect.value = RectF(0f, 0f, 1f, 1f)
        _cropRatio.value = CropRatio.FREE
        _rotationDegrees.value = 0f
        _flipHorizontal.value = false
        _flipVertical.value = false
        _currentScreen.value = AppScreen.SINGLE_EDITOR // Instant navigation!

        viewModelScope.launch {
            val context = getApplication<Application>()
            val (name, size) = ImageProcessor.getFileNameAndSize(context, uri)
            _singleFileName.value = name
            _singleFileSize.value = size

            val bitmap = ImageProcessor.loadBitmap(context, uri)
            _singleBitmap.value = bitmap
            if (bitmap != null) {
                _originalDimensions.value = Pair(bitmap.width, bitmap.height)
                _resizeConfig.value = ResizeConfig(
                    targetWidth = bitmap.width,
                    targetHeight = bitmap.height
                )
            }

            val exif = ExifHelper.extractExif(context, uri)
            _exifData.value = exif
            _isSingleProcessing.value = false
            scheduleEstimateSize()
        }
    }

    fun scheduleEstimateSize() {
        val bitmap = _singleBitmap.value ?: return
        estimateJob?.cancel()
        estimateJob = viewModelScope.launch {
            _isEstimatingSize.value = true
            kotlinx.coroutines.delay(120)
            val bytes = ImageProcessor.estimateCompressedSizeBytes(
                source = bitmap,
                cropRectNormalized = _cropRect.value,
                resizeConfig = _resizeConfig.value,
                format = _selectedFormat.value,
                quality = _quality.value
            )
            _estimatedOutputSizeBytes.value = bytes
            _isEstimatingSize.value = false
        }
    }

    fun setCropRatio(ratio: CropRatio) {
        _cropRatio.value = ratio
        val currentBitmap = _singleBitmap.value ?: return
        val targetRatio = ratio.ratio
        if (targetRatio == null) {
            _cropRect.value = RectF(0f, 0f, 1f, 1f)
        } else {
            val imageAspect = currentBitmap.width.toFloat() / currentBitmap.height.toFloat()
            if (imageAspect > targetRatio) {
                val newW = targetRatio / imageAspect
                val left = (1f - newW) / 2f
                _cropRect.value = RectF(left, 0f, left + newW, 1f)
            } else {
                val newH = imageAspect / targetRatio
                val top = (1f - newH) / 2f
                _cropRect.value = RectF(0f, top, 1f, top + newH)
            }
        }
        scheduleEstimateSize()
    }

    fun updateCropRect(rect: RectF) {
        _cropRect.value = rect
        scheduleEstimateSize()
    }

    fun rotate90() {
        _rotationDegrees.value = (_rotationDegrees.value + 90f) % 360f
        scheduleEstimateSize()
    }

    fun toggleFlipHorizontal() {
        _flipHorizontal.value = !_flipHorizontal.value
    }

    fun toggleFlipVertical() {
        _flipVertical.value = !_flipVertical.value
    }

    fun setResizeConfig(config: ResizeConfig) {
        _resizeConfig.value = config
        scheduleEstimateSize()
    }

    fun setSelectedFormat(format: ImageFormat) {
        _selectedFormat.value = format
        scheduleEstimateSize()
    }

    fun setQuality(q: Int) {
        _quality.value = q
        scheduleEstimateSize()
    }

    fun setStripMetadata(strip: Boolean) {
        _stripMetadata.value = strip
    }

    fun saveSingleImage(onSuccess: (ProcessedImageEntity) -> Unit) {
        val uri = _singleImageUri.value ?: return
        _isSingleProcessing.value = true

        viewModelScope.launch {
            val result = repository.processAndSaveImage(
                sourceUri = uri,
                format = _selectedFormat.value,
                quality = _quality.value,
                cropRectNormalized = _cropRect.value,
                rotationDegrees = _rotationDegrees.value,
                flipHorizontal = _flipHorizontal.value,
                flipVertical = _flipVertical.value,
                resizeConfig = _resizeConfig.value,
                stripMetadata = _stripMetadata.value
            )
            _isSingleProcessing.value = false
            if (result != null) {
                _toastMessage.value = "Photo saved successfully to in-app gallery!"
                onSuccess(result)
            } else {
                _toastMessage.value = "Failed to process photo."
            }
        }
    }

    // Batch Methods
    fun loadBatchImages(uris: List<Uri>) {
        val context = getApplication<Application>()
        val items = uris.map { uri ->
            val (name, size) = ImageProcessor.getFileNameAndSize(context, uri)
            BatchItem(
                id = UUID.randomUUID().toString(),
                uri = uri,
                fileName = name,
                originalSizeBytes = size,
                status = BatchItemStatus.PENDING
            )
        }
        _batchItems.value = items
        _currentScreen.value = AppScreen.BATCH_PROCESS
    }

    fun setBatchFormat(format: ImageFormat) {
        _batchFormat.value = format
    }

    fun setBatchQuality(q: Int) {
        _batchQuality.value = q
    }

    fun setBatchCropRatio(ratio: CropRatio) {
        _batchCropRatio.value = ratio
    }

    fun setBatchResizeConfig(config: ResizeConfig) {
        _batchResizeConfig.value = config
    }

    fun setBatchStripMetadata(strip: Boolean) {
        _batchStripMetadata.value = strip
    }

    fun startBatchProcessing() {
        val items = _batchItems.value
        if (items.isEmpty() || _isBatchProcessing.value) return

        _isBatchProcessing.value = true
        _showBatchProgressDialog.value = true
        val batchId = UUID.randomUUID().toString()

        batchJob = viewModelScope.launch {
            val updatedItems = items.toMutableList()

            for (i in updatedItems.indices) {
                val currentItem = updatedItems[i]
                updatedItems[i] = currentItem.copy(status = BatchItemStatus.PROCESSING)
                _batchItems.value = updatedItems.toList()

                val context = getApplication<Application>()
                val dimensions = ImageProcessor.getImageDimensions(context, currentItem.uri)
                val cropRect = if (_batchCropRatio.value != CropRatio.FREE) {
                    ImageProcessor.calculateBatchCropRect(
                        dimensions.width,
                        dimensions.height,
                        _batchCropRatio.value
                    )
                } else null

                val result = repository.processAndSaveImage(
                    sourceUri = currentItem.uri,
                    format = _batchFormat.value,
                    quality = _batchQuality.value,
                    cropRectNormalized = cropRect,
                    rotationDegrees = 0f,
                    flipHorizontal = false,
                    flipVertical = false,
                    resizeConfig = _batchResizeConfig.value,
                    stripMetadata = _batchStripMetadata.value,
                    batchId = batchId
                )

                if (result != null) {
                    updatedItems[i] = currentItem.copy(
                        status = BatchItemStatus.SUCCESS,
                        resultEntity = result
                    )
                } else {
                    updatedItems[i] = currentItem.copy(
                        status = BatchItemStatus.ERROR,
                        errorMessage = "Processing failed"
                    )
                }
                _batchItems.value = updatedItems.toList()
            }

            _isBatchProcessing.value = false
            _toastMessage.value = "Batch processing finished!"
        }
    }

    fun cancelBatchProcessing() {
        batchJob?.cancel()
        _isBatchProcessing.value = false
        _showBatchProgressDialog.value = false
        _toastMessage.value = "Batch processing cancelled."
    }

    fun dismissBatchProgressDialog() {
        _showBatchProgressDialog.value = false
        _currentScreen.value = AppScreen.GALLERY
    }

    // Gallery Methods
    fun selectGalleryItem(item: ProcessedImageEntity?) {
        _selectedGalleryItem.value = item
    }

    fun setGalleryFormatFilter(filter: String?) {
        _galleryFormatFilter.value = filter
    }

    fun toggleGallerySelection(id: Long) {
        val current = _galleryMultiSelectedIds.value.toMutableSet()
        if (current.contains(id)) {
            current.remove(id)
        } else {
            current.add(id)
        }
        _galleryMultiSelectedIds.value = current
    }

    fun clearGallerySelection() {
        _galleryMultiSelectedIds.value = emptySet()
    }

    fun deleteGalleryItem(item: ProcessedImageEntity) {
        viewModelScope.launch {
            repository.deleteImage(item)
            if (_selectedGalleryItem.value?.id == item.id) {
                _selectedGalleryItem.value = null
            }
            _toastMessage.value = "Photo deleted."
        }
    }

    fun deleteSelectedGalleryItems() {
        val selectedIds = _galleryMultiSelectedIds.value
        val itemsToDelete = galleryImages.value.filter { it.id in selectedIds }
        viewModelScope.launch {
            repository.deleteImages(itemsToDelete)
            _galleryMultiSelectedIds.value = emptySet()
            _toastMessage.value = "Deleted ${itemsToDelete.size} photos."
        }
    }

    fun exportGalleryItemToPictures(item: ProcessedImageEntity) {
        viewModelScope.launch {
            val success = repository.exportToGallery(item)
            if (success) {
                _toastMessage.value = "Saved to Pictures/PhotoForge!"
            } else {
                _toastMessage.value = "Export failed."
            }
        }
    }

    fun exportSelectedGalleryItems() {
        val selectedIds = _galleryMultiSelectedIds.value
        val itemsToExport = galleryImages.value.filter { it.id in selectedIds }
        viewModelScope.launch {
            var exportedCount = 0
            for (item in itemsToExport) {
                if (repository.exportToGallery(item)) {
                    exportedCount++
                }
            }
            _galleryMultiSelectedIds.value = emptySet()
            _toastMessage.value = "Exported $exportedCount photos to Pictures/PhotoForge."
        }
    }

    fun getShareIntent(item: ProcessedImageEntity) = repository.createShareIntent(item)

    fun getShareSelectedIntent(): android.content.Intent? {
        val selectedIds = _galleryMultiSelectedIds.value
        val itemsToShare = galleryImages.value.filter { it.id in selectedIds }
        return repository.createShareMultipleIntent(itemsToShare)
    }
}
