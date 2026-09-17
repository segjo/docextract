## Convert to PDF

Converts documents to PDF using LibreOffice. Accepts Microsoft Office (Word, Excel, PowerPoint), OpenDocument, plain text, and many other formats. You can upload one or more files per request.

Sections marked (Libre O ffice) apply during conversion. Sections marked (PDF Engines) post-process the resulting PDF with external tools such as QPDF, pdfcpu, and PDFtk; see PDF Engines .

See the Libre O ffice module configuration for startup and behavior flags.

## Basics

## POST

/forms/libreoffice/convert

## HEADERS

Gotenberg-Output-Filename string

The filename of the resulting file - Gotenberg automatically appends the file extension. Defaults to a random UUID filename.

## Gotenberg-Trace

string

A custom request ID to identify the request in the logs; overrides the default UUID.

## FORM   FILES

files file[]

At least one file to convert to PDF.

## EXAMPLE   REQUEST

cURL

REQUIRED

```
curl \ --request POST http://localhost:3000/forms/libreoffice/convert \ --form files=@/path/to/docx \ -o my.pdf
```

## RESPONSES

200

400

500

503

The PDF file. Multiple input files produce a ZIP archive.

```
Content-Disposition: attachment; filename={output-filename.ext} Content-Type: {content-type} Content-Length: {content-length} Gotenberg-Trace: {trace} Body: {output-file}
```

## Deciding Whether to Retry

LibreOffice reports a failure without naming its cause, so Gotenberg blames the request only when one of its inputs is implicated.

A 400 means a form field or the document explains the failure: a malformed nativePageRanges , a missing or wrong password , a document supplied with a password it does not need, or a document LibreOffice cannot read or convert. The error names the field to correct.

A 500 means nothing in the request explains it, so the conversion failed on the server. Large documents under memory pressure land here. Retry these, and increase the container's memory and CPU if they persist.

Cap the attempts either way. Some documents fail on every attempt, and Gotenberg cannot always tell those apart from a transient failure.

## Rendering Behavior

## Layout &amp; Pages

FORM   FIELDS landscape boolean Sets the paper orientation to landscape. DEFAULT: false singlePageSheets boolean Ignores each sheet's paper size, print ranges and shown/hidden status and puts every sheet (even hidden sheets) on exactly one page. DEFAULT: false skipEmptyPages boolean Suppresses automatically inserted empty pages. Active only for Writer documents. DEFAULT: false exportPlaceholders boolean Exports placeholder fields as visual markings only (non-functional). DEFAULT: false

## EXAMPLE   REQUEST

```
cURL curl \ --request POST http://localhost:3000/forms/libreoffice/convert \ --form files=@/path/to/document.docx \ --form landscape=true \ -o my.pdf
```

## Supported Extensions

| FAMILY             | SUPPORTED EXTENSIONS                                                                                                                                                                                                   |
|--------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| Word Processing    | .doc , .docx , .docm , .dot , .dotm , .dotx , .odt , .fodt , .ott , .rtf , .txt , .wps , .wpd , .pages , .abw , .zabw , .lwp , .mw , .mcw , .hwp , .sxw , .stw , .sgl , .vor , .602 , .bib , .xml , .cwk , .psw , .uof |
| Spreadsheets       | .xls , .xlsx , .xlsm , .xlsb , .xlt , .xltm , .xltx , .xlw , .ods , .fods , .ots , .csv , .numbers , .123 , .wk1 , .wks , .wb2 , .dbf , .dif , .slk , .sxc , .stc , .uos , .pxl , .sdc                                 |
| Presentations      | .ppt , .pptx , .pptm , .pot , .potm , .potx , .pps , .odp , .fodp , .otp , .key , .sxi , .sti , .uop , .sdd , .sdp , .fopd                                                                                             |
| Graphics & Drawing | .odg , .fodg , .otg , .vsd , .vsdx , .vsdm , .vdx , .cdr , .svg , .svm , .wmf , .emf , .cgm , .dxf , .std , .sxd , .pub , .wpg , .sda , .odd , .met , .cmx , .eps                                                      |
| Images             | .jpg , .jpeg , .png , .bmp , .gif , .tif , .tiff , .pbm , .pgm , .ppm , .xbm , .xpm , .pcx , .pcd , .pct , .psd , .tga , .ras , .pwp                                                                                   |
| Web & Other        | .html , .htm , .xhtml , .epub , .pdf , .pdb , .ltx , .mml , .smf , .sxm , .sxg , .oth , .odm , .swf                                                                                                                    |

LibreOffice is not a  1:1  clone of Microsoft Office. Documents with complex styling, Smart Art, or highly specific formatting may render slightly differently.

## Fonts &amp; Layout Fidelity

LibreOffice relies on installed fonts to calculate page breaks and layout. Missing fonts are the most common cause of layout issues and shifted pagination.

Install all required fonts in your Docker image (via a custom Dockerfile ). See Fonts configuration .

## Notes &amp; Slides

## FORM   FIELDS

exportNotes boolean

Exports notes to PDF.

DEFAULT:

false

exportNotesPages

## boolean

Exports notes pages to PDF. Available in Impress documents only.

DEFAULT:

false

## exportOnlyNotesPages

boolean

When exportNotesPages is true, exports only the notes pages.

DEFAULT:

false

## exportNotesInMargin

boolean

Exports notes in margin to PDF.

DEFAULT:

false

## exportHiddenSlides boolean

Exports Impress slides that are not included in slide shows.

DEFAULT:

false

## Links

## FORM   FIELDS

## convertOooTargetToPdfTarget boolean

Changes .od[tpgs] extensions to .pdf in exported links. The source document remains untouched.

DEFAULT:

false

## exportLinksRelativeFsys boolean

Exports file:// hyperlinks as relative to the source document location.

DEFAULT:

false

## Macros

Macros in documents (e.g., .docm , .xlsm , .pptm ) are disabled by default during conversion. Files are still converted, but macro-generated content will not execute.

## Linked Content

Documents can reference content by link instead of storing it: OOXML images marked TargetMode="External" , RTF INCLUDEPICTURE , ODT linked images.

Since 8.34.0 , LibreOffice drops linked content: an uploaded document always loads from an untrusted location, so linked file:// paths and external URLs are not resolved. The conversion still returns 200 OK with the linked content absent. There is no opt-out; embedded content is unaffected, so embed images instead of linking them.

Since 8.32.0 , any outbound fetch LibreOffice still performs routes through the same pinning proxy used by Chromium. Allow/deny lists and IP-class checks apply. The proxy rejects forbidden destinations inside the conversion, which succeeds without the resource.

[See Outbound URL Filtering .](https://gotenberg.dev/docs/outbound-url-filtering)

## Structure &amp; Metadata

## Document Outline (LibreOffice)

Configure how LibreOffice generates PDF bookmarks (the sidebar) and internal indexes like the Table of Contents. Your source document must use proper heading styles (e.g., Heading  1 , Heading  2 ) or explicit bookmarks to generate the hierarchy.

FORM   FIELDS

## updateIndexes boolean Updates indexes (e.g., Table of Contents) before conversion. May cause missing links. DEFAULT: true exportBookmarks boolean Exports bookmarks to the PDF. DEFAULT: true exportBookmarksToPdfDestination boolean Exports bookmarks as Named Destinations. DEFAULT: false addOriginalDocumentAsStream boolean Embeds the original document as a stream in the PDF for archiving. DEFAULT: false

## EXAMPLE   REQUEST

```
cURL curl \ --request POST http://localhost:3000/forms/libreoffice/convert \ --form files=@/path/to/document.docx \ --form exportBookmarks=false \ --form exportBookmarksToPdfDestination=true \ --form updateIndexes=false \ -o my.pdf
```

## Native Flattening (LibreOffice)

Set exportFormFields to false to prevent LibreOffice from creating interactive PDF widgets, effectively flattening them during conversion. Faster than post-processing.

This also fixes clipping when form fields reference fonts that aren't embedded:

LibreOffice sizes the widget boxes with a fallback font, so they come out too small.

Flattening removes the widgets and the mismatch.

Exports form fields as interactive widgets. Set to false to flatten them during conversion.

## FORM   FIELDS exportFormFields boolean DEFAULT: true allowDuplicateFieldNames boolean Allows multiple exported form fields to share the same field name. DEFAULT: false

## EXAMPLE   REQUEST

```
cURL curl \ --request POST http://localhost:3000/forms/libreoffice/convert \ --form files=@/path/to/document.docx \ --form exportFormFields=false \ -o my.pdf
```

## Metadata (PDF Engines)

Inject XMP metadata (Author, Title, Copyright, Keywords, etc.) into the PDF as a JSON object.

Not all tags are writable. Gotenberg uses ExifTool under the hood. See the XMP Tag Name documentation (https://exiftool.org/TagNames/XMP.html#pdf) for valid keys. Writing metadata usually breaks PDF/A compliance.

A key that is also an ExifTool option name, such as csv , json , o , or config , returns 400 Bad Request . Prefix it with a group, such as XMP:csv , to write it as a tag.

```
FORM   FIELDS metadata json Writes metadata (Author, Title, etc.). DEFAULT: None
```

## EXAMPLE   REQUEST

```
cURL curl \ --request POST http://localhost:3000/forms/libreoffice/convert \ --form files=@/path/to/document.docx \ --form 'metadata={"Author":"Julien Neuhart","Copyright":"Julien Neuhart 18T16:27:50-04:00","Creator":"Gotenberg","Keywords":["first","second"] 18T16:27:5004:00","PDFVersion":1.7,"Producer":"Gotenberg","Subject":"Sample","Titl \ -o my.pdf
```

## Attachments (PDF Engines)

Attach external files directly inside the PDF container. Commonly used for e-invoicing standards like ZUGFeRD / Factur-X (https://fnfe-mpe.org/factur-x/) , which require a machine-readable XML invoice as an attachment.

Provide per-attachment metadata with embedsMetadata to satisfy PDF/A- 3  and Factur-X requirements: each entry sets the embedded file stream's /Subtype , writes /AFRelationship on the file specification, and references the attachment from the Document Catalog's /AF array.

## FORM   FIELDS

```
embedsMetadata json
```

Per-attachment metadata keyed by filename. Each entry accepts mimeType (written to the embedded file stream's /Subtype ) and relationship (the /AFRelationship value, e.g., Source , Data , Alternative , Supplement , Unspecified (QPDF by default).

```
). Requires a PDF engine that supports the feature DEFAULT: None FORM   FILES embeds file[] Embeds files into the PDF. DEFAULT: None
```

## EXAMPLE   REQUEST

```
cURL curl \ --request POST http://localhost:3000/forms/libreoffice/convert \ --form files=@/path/to/document.docx \ --form embeds=@factur-x.xml \ --form embeds=@logo.png \ --form embedsMetadata='{"factur-x.xml": {"mimeType":"text/xml","relationship":"Alternative"},"logo.png": {"mimeType":"image/png","relationship":"Supplement"}}' \ -o my.pdf
```

## Factur-X (PDF Engines)

Produces a Factur-X / ZUGFeRD (https://fnfe-mpe.org/factur-x/) e-invoice in one request: upload the CII invoice XML with facturxXml and set facturxConformanceLevel . Both fields are required together. Gotenberg embeds the XML into the resulting PDF and never converts or merges it like other uploaded files. Requires a PDF engine that supports the feature (QPDF by default). See PDF Engines module configuration .

See the dedicated Factur-X route for the full behavior: canonical embedding, XMP metadata, and the PDF/A-3 requirement.

FORM   FIELDS facturxConformanceLevel enum The Factur-X conformance level recorded in the XMP metadata. Options: MINIMUM , BASIC WL , BASIC , EN 16931 , EXTENDED , XRECHNUNG . DEFAULT: None facturxDocumentType enum The Factur-X document type. Options: INVOICE , ORDER , ORDER\_RESPONSE , ORDER\_CHANGE . DEFAULT: INVOICE facturxVersion string The Factur-X version recorded in the XMP metadata. DEFAULT: 1.0 FORM   FILES facturxXml file The Factur-X CII invoice XML. Embedded as factur-x.xml regardless of the uploaded filename. DEFAULT: None

## EXAMPLE   REQUEST

```
cURL curl \ --request POST http://localhost:3000/forms/libreoffice/convert \ --form files=@/path/to/invoice.docx \ --form facturxXml=@/path/to/factur-x.xml \ --form 'facturxConformanceLevel=EN 16931' \ -o my.pdf
```

## Flatten (PDF Engines)

Merges all interactive form fields (text inputs, checkboxes, etc.) into the page content, making the PDF non-editable.

Converts form fields into static content, preventing further editing.

```
FORM   FIELDS flatten boolean DEFAULT: false
```

## EXAMPLE   REQUEST

```
cURL curl \ --request POST http://localhost:3000/forms/libreoffice/convert \ --form files=@/path/to/document.docx \ --form flatten=true \ -o my.pdf
```

## Merge (PDF Engines)

Converts each file to PDF individually, then merges them in alphanumeric order (numbers first, then alphabetical).

## FORM   FIELDS

merge boolean

Merge the resulting PDFs.

## EXAMPLE   REQUEST

cURL

```
curl \ --request POST http://localhost:3000/forms/libreoffice/convert \ --form files=@/path/to/1_document.docx \ --form files=@/path/to/2_document.docx \ --form merge=true \ -o my.pdf
```

## Split &amp; Page Ranges

## Native Printing (LibreOffice)

Happens during the conversion. Applied independently to each uploaded file.

Page ranges to print, e.g., 1-4 . Empty means all pages. Applied independently to each file.

```
FORM   FIELDS nativePageRanges string DEFAULT: All pages
```

## EXAMPLE   REQUEST

```
cURL curl \ --request POST http://localhost:3000/forms/libreoffice/convert \ --form files=@/path/to/document.docx \ --form nativePageRanges=1-5 \ -o my.pdf
```

## Post-Processing (PDF Engines)

Splits pages after the PDF has been generated using a PDF engine (pdfcpu, QPDF, or PDFtk). Applied to each file individually, or to the combined output after the merge operation.

When splitMode is set to pages , Gotenberg does not validate the splitSpan syntax. The value is passed directly to the underlying PDF engine, and the valid syntax depends on which engine you have configured:

| ENGINE           | SYNTAX REFERENCE                                                                                   |
|------------------|----------------------------------------------------------------------------------------------------|
| pdfcpu (Default) | See pdfcpu /trim documentation (https : //pdfcpu.io/core/trim)                                     |
| QPDF             | See QPDF page-ranges documentation (https : //qpdf.readthedocs.io/en/stable/cli.html# page-ranges) |
| PDFtk            | See PDFtk cat operation (https://www.pdflabs.com/docs/pdftk-man-page/#dest-op-cat)                 |

Check the PDF Engines module configuration to see which engine is active.

## FORM   FIELDS

splitMode enum

Either intervals or pages . Configures how the PDF should be split.

DEFAULT:

None

## splitSpan

string

The intervals or page ranges to extract, based on the splitMode.

DEFAULT:

None

## splitUnify

boolean

If true, puts extracted pages into a single file. Only works with pages mode.

DEFAULT:

false

## EXAMPLE   REQUEST

```
cURL curl \ --request POST http://localhost:3000/forms/libreoffice/convert \ --form files=@/path/to/document.docx \ --form splitMode=intervals \ --form splitSpan=1 \ -o my.zip
```

## Watermark &amp; Stamp

## Native Watermarks (LibreOffice)

Text-only watermarks applied during conversion. For image or PDF watermarks, use the PDF Engines watermark below.

```
FORM   FIELDS nativeWatermarkText string A single-line text watermark rendered behind the document content. DEFAULT: None nativeWatermarkColor integer The color of the native watermark text as a decimal RGB value (e.g.,  16711680  for red). DEFAULT: 8388223 nativeWatermarkFontHeight integer The font height (size) in points.  0  means auto-sized. DEFAULT: 0 nativeWatermarkRotateAngle integer The rotation angle in tenths of a degree (e.g.,  450  =  45°). DEFAULT: 0
```

nativeWatermarkFontName string The font name for the watermark text (e.g., Times New Roman ). DEFAULT: Helvetica

## nativeTiledWatermarkText string A tiled (repeating) text watermark rendered behind the document content. DEFAULT: None

## EXAMPLE   REQUEST

```
cURL curl \ --request POST http://localhost:3000/forms/libreoffice/convert \ --form files=@/path/to/document.docx \ --form nativeWatermarkText=CONFIDENTIAL \ --form nativeWatermarkFontHeight=48 \ --form nativeWatermarkRotateAngle=450 \ -o my.pdf
```

## Watermark (PDF Engines)

image and pdf sources require an uploaded watermark file. watermarkExpression cannot reference an arbitrary filesystem path; the route returns 400 Bad Request .

## FORM   FIELDS watermarkSource enum The watermark source type. Options: text , image , pdf . DEFAULT: None watermarkExpression string , the filename of the

The watermark content. For text , the string to render. For image or pdf uploaded watermark file.

DEFAULT:

```
None
```

Advanced watermark options in JSON format (e.g., font, color, rotation, opacity, scaling).

```
watermarkPages string Page ranges to watermark (e.g., 1-3 , 5 ). Empty means all pages. DEFAULT: None watermarkOptions json DEFAULT: None FORM   FILES watermark file An image or PDF file used as watermark source. DEFAULT: None
```

## EXAMPLE   REQUEST

```
cURL curl \ --request POST http://localhost:3000/forms/libreoffice/convert \ --form files=@/path/to/document.docx \ --form watermarkSource=text \ --form watermarkExpression=CONFIDENTIAL \ --form 'watermarkOptions={"opacity":0.25,"rotation":45}' \ -o my.pdf
```

## Stamp (PDF Engines)

image and pdf sources require an uploaded stamp file. stampExpression cannot reference an arbitrary filesystem path; the route returns 400 Bad Request .

```
FORM   FIELDS
```

```
stampSource enum The stamp source type. Options: text , image , pdf .
```

DEFAULT: None stampExpression string The stamp content. For text , the string to render. For image or pdf , the filename of the uploaded stamp file. DEFAULT: None stampPages string Page ranges to stamp (e.g., 1-3 , 5 ). Empty means all pages. DEFAULT: None stampOptions json Advanced stamp options in JSON format (e.g., font, color, rotation, opacity, scaling). DEFAULT: None FORM   FILES stamp file An image or PDF file used as stamp source. DEFAULT: None

## EXAMPLE   REQUEST

```
cURL curl \ --request POST http://localhost:3000/forms/libreoffice/convert \ --form files=@/path/to/document.docx \ --form stampSource=text \ --form stampExpression=APPROVED \ --form 'stampOptions={"opacity":0.5,"rotation":0}' \ -o my.pdf
```

## Rotate (PDF Engines)

Rotates pages by 90 , 180 , or 270 degrees during post-processing.

```
FORM   FIELDS rotateAngle enum The rotation angle. Options: 90 , 180 , 270 . rotatePages string Page ranges to rotate (e.g., 1-3 , 5 ). Empty means all pages. EXAMPLE   REQUEST
```

```
cURL curl \ --request POST http://localhost:3000/forms/libreoffice/convert \ --form files=@/path/to/document.docx \ --form rotateAngle=90 \ -o my.pdf
```

## Image Compression

## Compress Images (LibreOffice)

- Lossless (PNG): Best for line art, diagrams, or images with flat colors. Enable with losslessImageCompression .
- Lossy (JPEG): The default. Best for photographs or complex gradients. Significantly smaller file size.

## FORM   FIELDS

## losslessImageCompression boolean

Uses lossless (PNG) compression instead of lossy (JPEG) for images.

DEFAULT:

false

## quality integer

JPEG export quality. Higher values produce better quality but larger files. Between  1  and  100 .

DEFAULT:

90

## reduceImageResolution boolean

Reduces each image to the resolution specified by maxImageResolution .

DEFAULT:

false

## maxImageResolution integer

Target resolution in DPI when reduceImageResolution is true. Possible values:  75 ,  150 ,  300 ,  600 , 1200.

DEFAULT:

300

## EXAMPLE   REQUEST

```
cURL curl \ --request POST http://localhost:3000/forms/libreoffice/convert \ --form files=@/path/to/document.docx \ --form losslessImageCompression=false \ --form quality=50 \ --form reduceImageResolution=true \ --form maxImageResolution=75 \ -o my.pdf
```

## Post-Processing (PDF Engines)

Reduces the resulting file size by re-encoding its images to JPEG during postprocessing. Text, vectors, fonts, and structure are left untouched.

FORM   FIELDS

```
optimizeImages boolean Re-encodes the images in the resulting PDF to reduce its file size. DEFAULT: false imageQuality integer The JPEG quality applied to each re-encoded image, between 1 and  100 . Only used if optimizeImages is true . DEFAULT: 80
```

## EXAMPLE   REQUEST

```
cURL curl \ --request POST http://localhost:3000/forms/libreoffice/convert \ --form files=@/path/to/document.docx \ --form optimizeImages=true \ --form imageQuality=80 \ -o my.pdf
```

## PDF/A &amp; PDF/UA

Use the pdfa and pdfua form fields to convert the PDF into a standardized format.

- PDF/A: Specialized for the digital preservation of electronic documents (Archival).
- PDF/UA: Specialized for Universal Accessibility, ensuring documents are accessible to assistive technologies.

LibreOffice rasterizes table cells that carry a background color, which costs accessibility ( Tables Rasterized to Images ).

![Image](data:image/png;base64,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)

pdfua cannot be combined with PDF/A-1b . LibreOffice disables the pairing, so pdfua=true has no effect when pdfa=PDF/A-1b . Use PDF/A-2b or PDF/A-3b for a PDF/UA-compliant result.

```
FORM   FIELDS
```

```
pdfa enum Converts to a specific PDF/A archival standard. Options: PDF/A-1b , PDF/A-2b , PDF/A-3b . DEFAULT: None pdfua boolean Enables PDF/UA (Universal Accessibility) compliance. DEFAULT: false
```

## EXAMPLE   REQUEST

```
cURL curl \ --request POST http://localhost:3000/forms/libreoffice/convert \ --form files=@/path/to/document.docx \ --form pdfa=PDF/A-1b \ --form pdfua=true \ -o my.pdf
```

## Encryption

## Open Document (LibreOffice)

The password form field opens a password-protected source file.

```
FORM   FIELDS password string The password for opening the source file. DEFAULT: None
```

## EXAMPLE   REQUEST

```
cURL curl \ --request POST http://localhost:3000/forms/libreoffice/convert \ --form files=@/path/to/document.docx \ --form password=foo \ -o my.pdf
```

## Post-Processing (PDF Engines)

Set passwords and permissions to control PDF access. The user password is required to open the PDF. The owner password grants full access and lifts the permission restrictions; when empty, it defaults to the user password.

Since 8.34.0 , an owner password alone produces an owner-only PDF: it opens without a password, but the document permissions apply. Permission restrictions require a userPassword or an ownerPassword , otherwise Gotenberg returns 400 Bad Request . Restrictions are advisory: viewers honor them, but they are not cryptographically enforced once the document opens.

Encryption strength (e.g., AES- 256 ) and permission handling depend on the active PDF engine: QPDF honors each permission individually, pdfcpu restricts all permissions if any one is denied, and PDFtk supports neither owner-only encryption nor permission restrictions. See PDF Engines module configuration .

## FORM   FIELDS

## userPassword string

The password required to open the PDF.

DEFAULT:

None

## ownerPassword string

The password granting full access; lifts the permission restrictions. Defaults to the user password.

DEFAULT:

None

## allowPrinting

boolean

Permits printing the document.

DEFAULT:

true

## allowCopying

boolean

Permits extracting text and graphics.

DEFAULT:

true

## allowModifying

boolean

Permits changing the document content.

DEFAULT:

true

## allowAnnotating

boolean

Permits adding or modifying annotations.

DEFAULT:

true

## allowFillingForms

boolean

Permits filling in form fields.

DEFAULT:

true

## allowAssembling boolean

Permits inserting, deleting, and rotating pages.

DEFAULT:

true

## EXAMPLE   REQUEST

cURL

```
curl \ --request POST http://localhost:3000/forms/libreoffice/convert \ --form files=@/path/to/document.docx \ --form userPassword=my_user_password \ --form ownerPassword=my_owner_password \ --form allowCopying=false \ -o my.pdf
```

## PDF Viewer Preferences

Control how PDF viewers display the document on open. These preferences are embedded during conversion and take effect when a reader (Adobe Acrobat, browser PDF viewer, etc.) opens the file.

```
FORM   FIELDS initialView integer Initial view when opening the PDF.  0: neither document outline nor thumbnails.  1: document outline visible.  2 : thumbnail images visible. DEFAULT: 0 initialPage integer The page on which the PDF opens. DEFAULT: 1 magnification integer Initial magnification level.  0 : default.  1 : fit page.  2 : fit width.  3 : fit visible.  4 : use zoom value. DEFAULT: 0 zoom integer Initial zoom percentage when magnification is set to  4 . DEFAULT: 100 pageLayout integer
```

Page layout.  0: default.  1 : single page.  2 : one column.  3 : two columns.

DEFAULT:

0

## firstPageOnLeft

boolean

Place the first page on the left when using two-column page layout.

DEFAULT:

false

## resizeWindowToInitialPage boolean

Resize the viewer window to the size of the first page.

DEFAULT:

false

## centerWindow

boolean

Center the viewer window on the screen.

DEFAULT:

false

## openInFullScreenMode boolean

Open the PDF in full-screen mode.

DEFAULT:

false

## displayPDFDocumentTitle

boolean

Display the document title in the viewer title bar instead of the filename.

DEFAULT:

true

## hideViewerMenubar

boolean

Hide the viewer menu bar.

DEFAULT:

false

## hideViewerToolbar

Hide the viewer toolbar.

DEFAULT:

false

## hideViewerWindowControls boolean

Hide the viewer window controls.

DEFAULT:

false

## useTransitionEffects

## boolean

Use transition effects when advancing slides in Impress presentations.

DEFAULT:

true

## openBookmarkLevels

boolean

integer

Number of bookmark levels to show when opening the PDF. -1 shows all levels.

DEFAULT: -1

## EXAMPLE   REQUEST

```
cURL curl \ --request POST http://localhost:3000/forms/libreoffice/convert \ --form files=@/path/to/document.docx \ --form initialView=1 \ --form magnification=2 \ --form displayPDFDocumentTitle=true \ --form hideViewerToolbar=true \ -o my.pdf
```

## What's Next?

Combine or transform the result in Manipulate PDFs , or deliver it asynchronously with webhooks .

![Image](data:image/png;base64,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bJIoiowxQRBEUUTEaDQKk3CS0WjMyspCxIGBAcZYbm5uIpEIh8OMMQAQRVEQBJghsix7PB5FUeCLoGnawMDA9evXJyYmYIYYDIZZs2YVFxfr9Xr4DEZHR5999tnR0VFIc7vd27ZtM5lMiJhIJLq7u8+dO9fd3Y2IAGAwGB577LGqqiqDwTAyMvJP//RPo6OjkFZUVPR3f/d3MEnTtAsXLrz44ouQZjKZNm3atHHjRgC4ePHiG2+80dPTA2mSJOn1esYYTKHT6Wprax0OBwBwzv1+/3/8x39YrdZ58+bBJEScmJgIBoNFRUVer7esrMxoNMKktra2//zP//R4PM8884wgCHCrrq6ul19+OTs7+6mnnsrOzoa0CxcuvPnmm+Xl5Q899JDVaoVJPp9v3759GzdufOihhwRBgAygaZrP5/P7/TU1NXa7HaZoaGi4fPlyVVXV2rVrTSZTKpX6l3/5l56eHrgVImZlZd13330PPfQQAMTj8aNHj7a2tm7durWsrAw+TjAY3L17dywWY4whItzGYrHcf//9lZWVhw8f9vl8ALB69erNmzfn5uY2NTWdPHkyEolYrdZdu3Y5nc7GxsZjx46pqsoYe/zxx3NycgAAEUdHR3//+9+HQiHGmMfj2blzZ3FxMQC0trY2NjZ2dXUBwM6dO71er16vT6VSzz//fEdHByJyzr/zne9UVFQAACJGo9FDhw6dPHmSMVZSUvLEE09MTEw0NDQMDQ0hoiRJsiyLooiImqYlEglN0wCgqKhox44dc+bM6e7u3r17N0ubN2/eM888I4oiTFJV9cyZM6+//rqmaTk5Odu2bcvJydm/f38gEGCMISLcBhGffPLJNWvWQFooFGpoaLh48SIAZGdnb9iwYd26dYIgAMDAwMDrr7/e2toKABUVFdu3bz916pTP54vH46IoPv7442azWRAEAEilUn6///Tp04yx4uLip556amJioqGhob+/X1GUhx9+eN26dYyxt99++9ixY+Pj4zab7emnn3a73YIgwBSc85aWlvr6+traWofDAYRMjyEiEDKjOOenTp363e9+B2lms7m6urqiosLlcomiCLcZHx9/7bXXWlpaUqkUpBUUFJSVlZWUlOTk5OTm5o6MjAwNDfX09BgMBs650WisrKx0OBxtbW3nz58XRVFV1ZKSkgULFthsNgAIhUInT57s7e29fv26yWTasmWL2WwuLCxUFOWv/uqvIE2WZa/XW1lZ6Xa7dTodACBic3NzXV0dY6ywsLC4uDg3N3fBggVFRUWiKKqqeubMmcbGxrGxMQAwGAyPPPKIy+UqLS3NysrSNO3GjRutra3RaFQQBLPZvHTp0sLCQvgy4JyHQqFAIAB3yGKxCIKQSqUURYnH45qmAQBjTFVVSZJUVU2lUqIoGgwGzjkAICJjTBRFTdMEQYjFYgaDAQA454wxVVVjsZjVah0fHxcEITs7W1XVVCoFAKqqplIpuEOSJJlMJkRUVVUURUEQUqlULBaDSTwNAGw2myzLqqqGw2Gj0QgAg4ODBoMBEVVVBQC9Xg8zRJZlj8ejKAp8EUKh0LVr14aHh2GGSJJUVFQ0e/Zsk8kEnwEi9vb2/vznP9c0DdLMZnNRURFjDBFjsf/PHpzHRnmfiQN/nvd9Z97xzPgcMx7f9gDGBsxhHGxzNCEQCHeamE2TdLvZdLfdJq0qa6Vu1e6uqvzx24Zs1ahaddtV0ytsWickDibBEEg5Bkg4AuYaGwNmbIxvhvExnuN93+/zk15pJCPwNimkmZbn84kMDw+Hw2EwSZK0YMGCxx57LCcnBwDa29t/8YtfjI2NgWnx4sVf/epXISEajfp8vu3bt4MpOzt7/fr1S5YsAQCfz7dr165gMAhTczgc//Ef/6GqKgDEYrGjR4++9dZbmZmZaWlpkGAYxujoaCwWmzdv3qpVq3JzcyVJgoQ333zz4MGDzz77bHV1Ndymr6+vsbHRMIwvfelL+fn5YDpw4MCuXbseeeSRhx56yGKxQML27dsPHTr03HPPzZ8/HxEhCRiG4fP5/H5/fX292+2GSbZv337hwoXa2trly5fb7XYhxL59+/bs2ROPxyGBiGRZLikp2bRp0/Tp0wEgGo3u27evra1t48aN5eXlcCdDQ0Nbt26NRCIAYLFYqqurJUmCSWw2W3l5eW5u7u7du30+HwDU1NSsX7/e5XK1tLQcPHgwHA6np6e/8MILubm5v/nNb06dOmUYBiJmZ2cjIpgMwxgdHdU0DQDy8/Mfe+yxiooKAGhra9u5c2d3dzcAbNmypa6uzmq1jo+Pv/LKKwMDAwBARN///vdzc3PBFIvFPvzwwzfffBMRPR5PfX09EW3fvn1wcBAAiAgSEFGSJKfTOWPGjMWLF8+cOdNisXR3d2/duhVNZWVlzz//vCzLkBCLxQ4dOtTc3ExEOTk5mzZtcjqdTU1NgUCAiBwOR3l5eUpKCtyqpqbG6/WCaXBw8LXXXrt69SoAeDyejRs3zp8/H0zj4+Nvvvnmxx9/DABVVVWbN2/euXPnmTNndF0HAJfLhSYAIKJoNDo2NgYAOTk5Tz75pMvl2rNnz9GjRxGxrq5u3bp1mqbt2LHj3Llzuq7X1dWtX78+IyMDEWESIURra2tjY2NDQ4PH4wHGpoZEBIzdU0KIw4cP/+///i+Y0tLSlixZUlVVVVBQIMsy3Mn58+d37NjR09MjhICE1NTUWbNmFRcXj4+PRyIRVVXj8bjD4XA6nUuXLrXZbJ2dnceOHbNYLJqm5ebmzp8/PzMzE0wfffTRuXPn2tvbFyxYMGfOHKfTWVJSIoT49re/DSZVVevq6qqrq0tKSiwWC5i6urp27tzZ2dmZmZlZUFBQUlKSl5dXWFhot9s7Ozvfeuutq1evGoaBiJWVlcuWLXO5XPn5+YgYjUb37t0biUQkSRJCzJkzp6KiQpIk+EsghAgGg4FAAD49IgIAu90eiUQAgIgAABEhgYgAABGJCAAQESYhIgBARCICAESEe4GIENFut7tcLkSUJElRFCIaGhoaGRlBRAAgIsMwEFGSpNTU1JSUFEQkolgsNjQ0JIRISUkBUywWs1gskiTBvaCqqtfrdTgc8Gc3Pj5++fLl3t5eIQTcC4g4bdq0mTNnZmZmwicjhGhtbf35z38Of4yiKDNnzly7du306dMVRQGAAwcO7NixY2JiAkyPPfbY2rVrIWF0dHT37t0ffPABmAoLCzdt2jRv3jwA2Llz5969e2OxGEytuLj4e9/7HpjGxsZaWlo+/vjjZcuWzZo1CxIMwwiFQu3t7ZcvX547d+7q1atdLheYhBBbt27t7+//wQ9+kJGRAbcZGBh44403YrHYk08+WVhYCABCiHfffffQoUNbtmxZvHgxIkLCT3/6U7/f//3vfz83NxeSg2EYPp/P7/fX19e73W6YZPv27RcuXKitrV2+fLndbieicDh89OjRlpYWXdcBgIgkSSosLNywYcOMGTMsFgsARKPRffv2tbW1bdy4sby8HO5kaGho69atkUgEAFJSUr7zne9YLBaYRJIkVVUjkcju3bt9Ph8A1NTUrF+/3uVytbS0HDx4MBwOp6env/DCC7m5ub/61a9aW1uFEIjo8XgQERIQEQCIKDs7+8EHHywrKwOAtra2nTt3dnd3A8CWLVvq6uqsVuvY2Ngrr7wyMDCAiET0b//2bzk5OWCKx+MffvjhG2+8gYgej+eJJ54AgO3btw8ODhKR2+2eOXOm0+lERIvFYrfbs7KycnJy0tPTFUVBxK6urq1bt6KprKzs+eefl2UZEm7cuLFnz56jR48CwIwZMzZt2kRETU1NgUCAiDwez5NPPul2u+FWKSkpqqqCaXBw8LXXXgsEAkSUm5u7cePGefPmgSkcDr/xxhsff/wxAFRVVW3evLm5ufns3f99IgAAIABJREFU2bO6riOix+NBRJgEEYkoKytr1apVxcXFx44da2pqikajpaWl69at6+/v37dv38jIiKIoTz31VFVVlcVigVsJIVpbWxsbGxsaGjweDzA2NSQiYOyeEkL4fL7XX38dTGlpaUuWLKmqqiooKJBlGe5E07SDBw/u37//xo0bRAQJFRUVWVlZdrs9Ho9bLJZ4PO50OisrK71eLwB0dXUdO3YMABAxKytrwYIFLpcLTDdu3Dh//vylS5fKy8vT0tLy8/OzsrJisdi3v/1tMKmqWldXV11dXVJSYrFYwKRpmt/vP3bsGCLm5ubm5OQ4nc6CgoLU1NQDBw60tLSEQiEAcLvdK1eudLvdXq/XZrMBwM2bN/ft26fruqIoqampdXV16enp8BdCCBEMBgOBAPypiAgRYWqISEQWi0UIYRgGTIKIiqLYbDZN0yKRCCLCFBRFQcR4PI6I8H8iIgCwmmRZTklJsdlssVgsGAxGo1FJkgDAMAwikmUZERVFsVqtQghd16Mmp9OJiABAREIIXdetVivcC6qqer1eh8MBf17xeDwQCFy9elXTNLhH0tLSZs6cmZOTI0kSfDKapu3du3fHjh3wf8rIyJgzZ05dXV1JSYnFYgFTY2Pj4cOH4/E4mF544YV58+ZBwvDw8Pbt20+fPg2m8vLyxx9/vLi4OB6Pv/nmmz6fj4gAQFXV6dOn2+12uFVpaemqVavAdPPmzddff/3GjRtPPfXUzJkz4VYDAwPNzc1Xr16tr6+fN2+eoigAEAqFfvjDHzqdzu9+97uKosBtent7f/e730mS9PTTT+fk5ABAOBxuamrq6Oj4m7/5m7lz50KCpmkvvfTSyMjID37wA4fDAcnBMAyfz+f3++vr691uN0yyffv2Cxcu1NbWLl++3G63AwARRSKRI0eO7Nq1S9M0SZIKCgo2b948ffp0RVHAFI1G9+3b19bWtnHjxvLycriToaGhrVu3RiIRAHA4HC+++KKqqnCbUCi0e/dun88HADU1NevXr3e5XC0tLQcPHgyHw+np6S+88EJubu7bb7/t8/k0TZMk6Vvf+pbD4YAEIgIARFQUJTU1NSUlBQDa2tp27tzZ3d0NAFu2bKmrq7NarfF4/Cc/+Ul3dzcRCSFeeOGF2bNngykSiezZs2fv3r2IWFhY+NRTT4XD4e3btw8ODhLR7Nmz16xZ4/F4AEAyySZEBFNXV9fWrVvRVFZW9vzzz8uyDCZd18+fP//OO+8MDw8jYl1d3bp1627cuNHU1BQIBIgoLy/vueeey83NhanduHGjsbHR7/cDQGZm5po1a5YtWwamoaGht9566/z58wBQVVW1efPmAwcOHD16NBaLWa3W559/3m63QwIRAQAiyrKcnp6uqmpnZ2dzc/OVK1ccDkdNTc3Q0NC5c+cAoLCw8KmnniosLEREuJUQorW1tbGxsaGhwePxAGNTQyICxu4pIYTP53v99dfBlJWVtWrVqhkzZhQUFMiyDFMIhUJnz571+Xw9PT1CCDCVlZWlp6fbbDa73R6Px2fOnJmZmZmfn6+qKgB0dXV99NFHhmFYLJacnJzZs2dnZ2eDiYhGRka6u7sBICUlpbi42Gq16rr+rW99SwgBAKqq1tXVVVdXl5SUWCwWSBgdHe3q6hobG0tJSVFVNS0tLTs722azHTx4cNeuXaFQCBEffvjhWbNmuVyu/Px8RASAkZGRlpYWMFVWVlZUVEiSBH8hhBDBYDAQCMCngYipqamIGIlEVFUVQiCiJEmGYYBJCGGxWAzDQERFUYgIEYVJ13Wr1YqIkUgEABwmXdfD4bCmaaqqxuNxi8UihDAMQ5bleDwuhEhLS9N1fWRkBBHh05BMRKTruhBClmUiMgwDEWVZRlNKSgoRhcNhTdMsFgsAoAkAiCgWiymKIssy3DVVVb1er8PhgD8jXdf7+/s7OjomJibgHrHZbF6vt7Cw0GKxwCcWi8W2bdt2/PhxMFkslrS0NFmWAUCSJFVVnU6n2+0uLi6eMWOGy+WSJAkSfvKTn7S3txuGAab/9//+n8vlgoTr16//4he/6O3tBVNNTc2WLVtSU1OHh4ffeuutU6dOgSkvL++LX/zitGnT4FYOhyMtLQ1MAwMD//3f/2232//xH/8xMzMTbnPw4MGWlpba2tqVK1empqYCwOXLl3/2s5+VlZU999xziqLAbbq6un7961+7XK6/+7u/S01NBYC+vr4dO3aEw+HHH3+8tLQUEoaGhl555ZWMjIxvfetbNpsNkoNhGD6fz+/319fXu91umGT79u0XLlyora1dvny53W4HExFFo9GjR4/u2bMnOzv7i1/8YmlpqaIokBCNRvft29fW1rZx48by8nK4k6Ghoa1bt0YiEQBwOBwvvviiqqpwm1AotHv3bp/PBwA1NTXr1693uVwtLS0HDx4Mh8Opqanf/OY3c3Nzz5079+tf/zoejxPRl7/85ZqaGkQUQgwODh44cAAAVFXNy8urrKx0OBwA0NbWtnPnzu7ubgDYsmVLXV2d1Wolot///vcnTpyIx+MAsGjRor/9279VFIWIBgcHX3311d7eXkmSZs+e/ZWvfKWrq2v79u2Dg4NEVFlZuWHDhvz8fJhCV1fX1q1b0TRjxoyvf/3riCiECIfDly9f9vl8165dE0JkZWWtXbu2pqbm6tWrTU1NgUCAiPLy8p577rnc3FyYhIjOnTsXiUSIyGazFRUVHTx4cN++fYioKEp1dfWmTZvS0tKEEKdOnWppaRkYGACAqqqqzZs3X7169a233hobGwOAZ599du7cuaqqElE8Hj927FhfX5+qqtOmTauurrbZbGNjY3v37t2/fz8iZmVlxePx0dFRAFhpSktLg9sIIVpbWxsbGxsaGjweDzA2NSQiYOyeEkK0t7f7fL7x8XFEzMvLKysrc7lcBQUFsizD1GKx2IULF86dO9fb2zsyMjIxMZGfn19SUiLLcl5eXk5OTl5eXkpKCiT09fVdvnxZURRN0/Ly8goLC1VVhUlGR0cnJiYcJkmSAODdd9/t7OyMRqMWi6XMVFJSYrFYIMEwjEgkEg6HDcNQVdXhcKiqiog9PT07duxoa2vLy8tbuXJlRkZGcXGxzWYDUywW8/l8Y2NjiqI89NBDqamp8JdDCBEMBgOBAHwaTqczxaRpmizLhmEg4vj4eFZWlhAiGo0SEQAYhpGSkiLL8tjYmKIoDodjdHSUiDIzMycmJuLxeDQaTU1NzcjIiMVi/f39GRkZVqt1cHAwKytLluVwOByPxy0WiyzLkiRFIpGxsTH4lIQQhmEoioKIhmEQEZhkWZYkSQjhdDpTUlKEEKOjo7quExEAoAkAiMgwDACQZRnumqqqXq/X4XDAnwsRBYPBtra2UCgE94iiKPn5+TNmzEhJSYFPIxqN/uhHP+ru7gZTbm7uo48+arfbAUCSJIvFYrPZMjIyUlNTJUmCSWKx2EsvvdTb20tEAGC32//zP/9TlmUwCSH8fv///M//xGIxALDZbKtWrdqwYQMidnZ2NjU1dXR0gGnBggVbtmzJzs6GqXV3d//oRz8qKyv7+te/rigK3Ob8+fNNTU2FhYUbN250uVwAcPLkyddff72uru7xxx+XZRluc+nSpZ/97GezZ89+9tlnZVkGgI6Ojh07drhcrg0bNrjdbkjw+/2/+c1v5syZ8+STT6qqCsnBMAyfz+f3++vr691uN0zy9ttvnz17dvHixQ8++KDD4YAEIorFYteuXXM4HDk5ObIswySRSGTv3r0dHR0bN26cNWsW3MnQ0NDWrVsjkQgAOByOF198UVVVuE0oFNq9e7fP5wOAmpqa9evXu1yuffv2/eEPfxgbG5MkaenSpTNnzpw+ffovfvGLq1evElFWVtaqVasKCwtDodCHH37Y0dEBAKqqVldXb9682Wq1AkBbW9vOnTu7u7sBYMuWLXV1dVarFQA6Ozu3bds2ODgIABaLZcmSJXPnzo1Go8eOHfP7/UKIjIyMtWvXLlmypL29ffv27YODg0RUWVm5YcOG/Px8mEJXV9fWrVsREQCcTmdJSQkAaJo2Pj4eCoUikYgQwmq11tXVPfLII+np6Z2dnU1NTYFAgIjy8vKee+653NxcmEQI8eMf/7i/v5+IsrOzv/71r3d3d//2t7+Nx+NElJqaOn/+/NmzZ4+Ojh4/fryrq8swDACoqqravHmz1Wr95S9/eeXKFSGEy+WaPn364sWLLRZLR0fHwYMHo9Go1WpduHDhli1bFEURQpw8eXLHjh0jIyOSJJEpJSXlmWeeqayslGUZbiOEaG1tbWxsbGho8Hg8wNjUkIiAsXttYmLixo0b169f1zTN4XDY7fasrCy32w1/jKZpQ0NDXV1dIyMjsVjMarXm5OS43W6n05mRkQG3isVi0WhUlmVd120muI2u67IsIyKYJiYment7+/r6iMhut6enp5eUlFgsFriVYRhCCEmSZFkGk2EYly5dOnbsWK4pJydn2rRpiAgmIcTw8PDAwEBaWlpRUREiwl8OIUQwGAwEAvBppKSkpKenO53O0dFRq9Wq6zoRBYNBj8djmOLxuBBCkiSHw2EYxsTEhKqqFoslHo9rmpaWljY6OoqIkUjEbrdnZmaGw+FYLCbLsqIoQ0NDHo8HAMbGxsLhcHp6usPhCIVChmGEw2FEhE+DiAzDQERFUQzDEEIAACLKsoym9PR0q9UaiUTC4bBhGJCAiABAplgsZrPZ4K6pqur1eh0OB/y5hMPhtra2gYEBIoJ7ARGzs7PLy8vT09PhUxobG/vXf/3XaDQKAIg4e/bsb3zjGxaLBf6Y3t7e//qv/7px4waYvF7vv/zLv0BCOBzet2/frl27wJSXl7dp06aFCxcCwOnTp5ubm3t7e8G0atWqRx99NDU1FaZgGMa5c+d++ctf1tbWPv3003AnFy5caGpqysvL27RpU3Z2NgAcOnSoqanpkUceefTRRyVJglsZhnHmzJnXXntt2bJlTzzxBJhOnjzZ3Nw8f/78NWvWOJ1OSDhw4MDOnTvXrFnz8MMPK4oCycEwDJ/P5/f76+vr3W43TLJ///59+/Z5PJ6nn37a5XJBAhEBABGhCW7V39//7rvvapq2efPmvLw8uJOhoaGtW7dGIhEAcDgcL774oqqqcJtQKLR7926fzwcANTU169evd7lc7e3tb7/9dm9vLyJKklRYWPi1r31tfHz81VdfHRgYQERFUWRZFkLoui6EkCRp1qxZjz/+uMfjQUQAaGtr27lzZ3d3NwBs2bKlrq7OarUCgGEYg4ODb7311sWLF4lIlmVFUYhI13UhhN1u/8IXvrB69Wqr1drW1rZ9+/bBwUEiqqys3LBhQ35+Pkyhq6tr69atiAgAiChJEgAQEQAIIQAgLS1t6dKly5YtS0tLQ8QrV640NTUFAgEiysvLe+6553Jzc2ESIcQPf/jD3t5eAJg2bVpDQ4MQ4oMPPjhw4AARAYBsIiKn02kYxvj4uBCiqqpq8+bNWVlZPT0927Zt6+3tBQBEtFqtAKCbJEmaMWPGk08+6Xa7EZGI+vr63nvvvTNnzkBCZWXlpk2bPB4PIsJthBCtra2NjY0NDQ0ejwcYmxoSETD2GTAMY2BgQNM0AFBVNTMzU1VV+AQMw5iYmBgfH5+YmIjH41ardfr06XDv6Lo+MDCg6zoAWK1Wt9styzJ8AoZhXLx4UdO0tLS0vLw8q9WKiJBARLFYzGq1SpIEf1GEEMFgMBAIwKeBiAUFBQDQ398vyzKZNE1TVVUIgYiGYQAAIsqyLIQgIkmSiEiSJMMwZFnWdR0AhBCSJFksFsMwJEnSdV2SpHg8npKSQkSapgFAWloaAIyOjgKAruuICJ8GmQBAlmUi0nXdMAzFREROp9Nms0mSFAqFNE2DSYgIEQFACBGNRlNSUuCuqarq9XodDgf8uWiadvPmTcMw4NMYGxvr6OiAO0lNTZ01a1ZOTg4iwqdBRF1dXf/xH/8BJqvV+sADD3zlK1+BT6C1tfX1118fGRkB0xe+8IVnnnkGTETU1dX161//uq+vDwAkSVq4cOGWLVsyMzMB4MCBA++9997o6CiYnnrqqbq6OlVVYQqxWOzgwYPvv//+ahPcydGjR3ft2jVv3rw1a9akp6cDwMGDB9955501a9asXr1akiS4VTgc/uCDD44dO7Z+/folS5aAaf/+/e+9996jjz66YsUKWZYh4c033zx8+PBXv/rVuXPnSpIEycEwDJ/P5/f76+vr3W43THL27Nm33347Ho//wz/8Q0lJiSRJ8McIIdra2pqbmwsLCzdu3Jieng53Mjg4+PLLL0ciESJyOp0vvviiqqpwm1AotHv3bp/PBwCLFy9ev359dnb2zZs333zzzfPnzwshAKCwsPAb3/iG3W7fv3//rl27NE2DW2VlZa1ataqurs5isYCpra1t586d3d3dALBly5a6ujqr1QoAZGptbX3rrbdCoRAiQoIkSTNnznzssccKCgoQsa2tbfv27QMDA0RUWVm5cePG/Px8mEIgENi6daskSXArIgKA1NTUBQsWLFu2LC8vT5IkALhy5UpTU9PVq1eJKD8//7nnnsvNzYVJhBA//OEPe3t7AWDatGkNDQ12u93v97/99ttDQ0OICAmVlZWRSOTq1au6rldVVW3evDk7O1vX9T/84Q/vv/9+NBqFW2VkZKxevXrJkiWKooBpbGyspaXl0KFDYCKihx9+eNWqVenp6XAnQojW1tbGxsaGhgaPxwOMTQ2JCBj7bMTjcSICAEmSFEVBRPhkhBCGYei6bhiGJEl2ux3uqXg8TkQAgIhWqxU+sXA4HAqF0tPTnU4n/LUQQgSDwUAgAJ+SzWYTQsTjcfgsIaKiKEIIwzDgT0VEQghFUQBA0zQhhMVikSRJCJGenm61WhExGAwahoGIkKBpmsViAQAiMgxDkiREhLujqqrX63U4HJDcLl68eOnSJbiNzWabPn16UVGRLMvwKQkhjh8//qtf/QpMTqdz5cqV69atg0/g/fff3717dzgcBtNTTz310EMPAYAQ4sqVK3v27Llw4YIQAgCys7PXrl27dOlSRCSi5ubm999/X9d1AJBl+Z/+6Z8qKysREaYQDoebmpra2tqeeOKJqqoquI2u601NTYcPH37iiSfq6uosFgsAHDt2rLGxcdmyZZs3b5ZlGW41MDCwbds2Xde//OUv5+fnA4Cu6++9996RI0eeeOKJmpoamOSnP/1pW1vb9773PY/Hg4iQHAzD8Pl8fr+/vr7e7XbDJLFYrLGx8fTp0263e/369V6vV1EURIQ7EUKMjIycPn36ww8/tFgsa9euXbRoESLCnYyOju7ZsycWiwGAqqpf/OIXFUWB20xMTJw7d+7SpUsAUFpaOm/evNTUVCIaGRnx+/3Xr1+Px+OZmZkPP/ywzWYTQly7du3YsWPXrl2LRCJWqzUnJ6esrKyoqMjj8ciyDAm9vb1nzpy5ceMGAFRXV8+YMUNRFEggoqGhoePHj1+5ciUcDiNidnb27Nmz582bl5qaCqa+vr6TJ0+Ojo4SUUFBwfz58zMzM2EKw8PDu3fvRkSYRFGU1NTUadOmFRUVZWVlWSwWSBgcHDx16tTw8DAAZGRkLFu2LCMjAyYhol27doVCISJKTU1dvXp1SkqKruv9/f3nzp3r7OwcHR11OBwVFRULFy4MBAKXLl0yDKO4uHjhwoVOpxMAhBBdXV0nTpwImgAgNTW1tLS0qqrK4/FIkgQJRNTR0dHa2qrrOgBIkrR48eLS0lJJkuBOhBCtra2NjY0NDQ0ejwcYmxoSETDGPrF4PK4oiiRJ8NdCCBEMBgOBAPz1IiIhhKIoACCEICJJkhBRkqTU1FSLxWIYxujoqK7riAgmRAwGg5mZmQBAJsMwFEWBu6OqqtfrdTgckMR0Xff5fOFwGG6lKEp+fn5ZWZmqqvDpCSHeeeedPXv2gMnlcj322GOLFy+GT2Dbtm3Hjh2Lx+NgWrVqVX5+/sjIyODgYFdX1+DgoKZpAKCq6gMPPLB58+a0tDQACIfDTU1Nhw8fJiIAyMzM/NrXvub1emFqoVDo1VdfHR8f//u///uioiK4TUdHxzvvvDMxMfHMM8/MmDEDEQGgs7Pzpz/9aVFR0fPPP68oCkyiadrHH3/89ttvV1VV1dfXK4oCACMjI83NzYFA4PHHH58zZw4kCCFeeumlmzdvfu9738vIyICkYRiGz+fz+/319fVutxtuNTw8fP78+YMHD46PjzscDkVREBHuRAgRjUYjkUhWVtaDDz5YU1NjtVphCmSCBEmS4E6ICACICAAQEQAQEUxEBJMgIpiEEPF4XAghSZKiKLIsIyLciojgVogItyKieDxuGAYiKiZEhAQiAhMRISIAICJMgUyICLdBRLgNmSABTTAJEYGJiAAATWASQui6bhiGJEkWi0WSJDIhIpgQERKEEIZhaJoGALIsW61WRITbEBHcChFhCkKI1tbWxsbGhoYGj8cDjE0NiQgYY/cxIUQwGAwEAvDXixIURTEMg4gURQEAi8XicDgsFks0Gg2Hw0IISEDErq6uoqIiACCieDwOAFarFe6Oqqper9fhcEAS6+/vP3XqlBACJpEkKSsra+7cuU6nE/4kuq7//Oc/P3v2LJhyc3O/8pWveL1e+AR+9KMfXb58WQgBprS0NFmWdV2PxWLxeBxMiqJUVFQ8/vjjubm5iAgA/f3977zzzunTp8E0ffr0p59+uqCgAKY2PDz88ssvZ2RkfPvb37bb7XCrnp6elpaWCxcuPPTQQytXrkxNTQVTLBZ76aWXgsHgd77znby8PEggomvXrr322mtE9PTTT3u9XjD19PTs2LHDMIzNmzcXFxdDgqZpL730UiQS+c53vpOeng5JwzAMn8/n9/vr6+vdbjfcRgjR19d35syZ3t5ewzBgCojodDpLS0unT5+elZWlKAqw+48QorW1tbGxsaGhwePxAGNTQyICxth9TAgRDAYDgQD8VRNCEJEkSUQkSRIiAoCiKE6nU1GUiYmJSCRCRJCAiO3t7bNmzQIAIorH45IkKYoCd0dVVa/X63A4IIl9/PHH/f39RASTOByOefPmZWVlISL8STRN+8EPfjA8PAym4uLib37zm2lpafDHjI6OvvLKK729vUQEU1BVde7cuWvXri0oKEBEMHV0dLzzzjtXrlwBU01NzaZNm7Kzs2EKRNTd3f3yyy/PmDHj2WefRUQwaZo2PDx86dIlv99//fr1OXPmrF+/Pj8/HxEhYffu3e+9957X6/3Sl76Um5sLAIZhXLp06d13371x48aqVasefPBBRVHA1N7e3tTUlJeXt2HDBpfLBQlE9PLLL/f09PzzP/9zUVERIkJyMAzD5/P5/f76+nq32w1TEEIYhkFEMDVJkmRZRkRg9yshRGtra2NjY0NDg8fjAcamhkQEjLH7mBAiGAwGAgH4q0ZEwoSIFosFTJIkOZ1Oq9UajUYnJiaEEJCAiG1tbeXl5QBARPF4XFEUSZLg7qiq6vV6HQ4HJKuJiYkjR47EYjGYxGq1lpeXFxQUSJIEf6rR0dHvfve7hmEAgCRJFRUV3/rWtxAR/pjOzs5f/vKXQ0NDcCeSJHk8nmXLllVXV6empkqSBAknT55sbm4eGBgA09q1a1euXJmamgpT0HX99OnTr776qqqqaWlpkEBEuq7HYjGr1bpgwYLly5fn5eVJkgSThMPh3/72txcuXEhPT8/JyXE4HKFQaHh4GABWrFjxhS98wWazQcKJEyeampoeeOCBtWvX2mw2mOSNN944cuRIenp6ZmbmwoULH3roIUgChmH4fD6/319fX+92u4GxuyCEaG1tbWxsbGho8Hg8wNjUkIiAMXYfE0IEg8FAIAB/7YgITJIkQYLNZrPb7ZqmhcNhIQQkIOL58+fnzJkDAESk67qiKHDXVFX1er0OhwOSVWdn58WLFw3DgARZlgsLCysqKmRZhrvQ29v72muvgUlRlNmzZ69duxY+gfb29j/84Q9jY2MwiaIo6enpWVlZXq+3pKTEbrdbrVa41alTpw4fPhyJRMC0evXquXPnWiwWmIKmaSdPnjx06BDcymKxuFyukpISr9frcrlsNpskSXCbkZGR999///jx4xMTE4jodDorKirq6uqKi4tVVYVJTp06dejQoaVLl1ZXVyMiTNLX1/f66693dnbKsvzkk08uXboUkoBhGD6fz+/319fXu91uYOwuCCFaW1sbGxsbGho8Hg8wNjUkImCM3ceEEMFgMBAIwH1ACAEAsizDJEQEJkSEBEQ8e/ZsZWUlAJAJEeGuqarq9XodDgckJSHE0aNHR0ZGiAhMiJiVlbVo0SKr1Qp3RwgRjUYhQVEUq9UKn4Cu65qmERHcSjLJJrgTXdfj8TgkWK1WRVHg/6SZ4DaSJCmKIssyIsLUYrFYPB6PRCJCCIfDYbFYrFarJElwK13X4/G41WpVFAVuRUSRSMQwDCJKSUmxWCyQBAzD8Pl8fr+/vr7e7XYDY3dBCNHa2trY2NjQ0ODxeICxqSERAWP3TjQajcViACBJUmpqKhHpum6xWIRJURQhRDQa1TRNlmWbzaYoCtxqcHAwNTU1JSUFTEQ0PDxst9sdDgcAEFE0GjUMw2azKYoCJl3XAUBRlFgsJsuyoiiapsmyLEkSABBRX19fenq6w+GASYhI13VZliVJIiLDMCQTAOi6DgCKokDCjRs3JElKT0+XJAkAYrGYqqoAEIvFVFUlong8rqoqEem6LsuyJEnCpChKzEREKSkpVqsVkowQIhgMBgIBuA8QEQAgIkxCRACAiHArTdMURYEERIS7pqqq1+t1OByQlIaGhlpbW2OxGCTYbLba2lqn0wnsPmYYhs/n8/v99fX1brcbGLsLQojW1tbGxsaGhgaPxwOMTQ2JCBi7dzo7O3t6ejo6OhYsWFBdXT3RYO4aAAAgAElEQVQyMtLW1lZbWzswMNDf3z9//vzu7u6PPvrIYrGUlJSUlpZmZGTAJHv27MnIyOju7p4+ffqCBQskSfrd735XXFwciUTy8vIqKipCodDx48dtNtvo6GhOTs4DDzwAAO3t7QBQXl7e0tISDocfeuihy5cvz5gxIzs7W9f15ubmoqKiYDBYWlo6c+ZMSIhEIidPnpQkaenSpRcvXjx27NjatWunTZsGAAcOHNA0bfny5TabDQB8Pp8kSYZh2O32yspKi8Wyc+fO9evXK4qyZ8+eNWvWRKPR/fv3r127NhwOHzt2TNf1VatWdXd3h8Ph2bNnnzx58tq1azk5OdOnT/d4PJBkhBDBYDAQCMB9gIgAABHhEyAimAQR4a6pqur1eh0OBySlc+fO9fT0GIYBJkRctGiRx+MBdn8zDMPn8/n9/vr6erfbDYzdBSFEa2trY2NjQ0ODx+MBxqaGRASM3VO6ru/fv/+RRx4BgFAodPr06RkzZgwPD0cikSVLlnR1dZ0+fdrtdldWVjqdTkSEhI6ODk3TZs6cabVaiQgRz58/L4SYN2/exMTE3r17N2/efPPmzYsXL9bW1o6PjwcCAbvd7vV629raAKCiouLdd98dGhqqra0dGBiYO3dudnb2Rx99VFRU5PF4JEkiIkSEhImJicOHDweDwXXr1rW2to6MjNTU1Ljd7mvXrg0PD09MTOTm5nq93q6urpGREa/X63Q6iQgRDcP4/e9/v3TpUlmWz507t27dukgksn///nXr1o2Pj586dSoajc6YMQMRw+HwnDlzWltbx8fHKyoq0tPTLRYLJBkhRDAYDAQCwD57qqp6vV6HwwHJJx6PHzlyJBwOQ4LX6509ezaw+55hGB999JHf79+4caPH4wHG7oIQ4tSpU++9997zzz8/bdo0YGxqSETA2D2l6/r+/fsfeeQRAAiFQidOnMjJyQmFQkKIhx56qKur68SJE2lpaQsXLszOzkZESDh//ryqqiUlJRaLhYgQ8eTJk+np6TNnzpyYmNi3b9+mTZtu3rx58eLF2traaDQaCAQQcdasWW1tbQBQUVHx7rvvFhUVjY6ODgwMPPjgg9nZ2UeOHCkvL8/Kyvrggw+i0eiGDRsgYWJiorW1VVGUmzdvappWUFCQl5c3bdq0Q4cOjY6OZmZmZmdnz5o168qVK5qmFRcX2+12IkJEwzC2bds2f/58SZJ6enrWrVsXiUT279+/bt268fHxixcvImJfX19mZmZaWtqcOXNOnTo1ODhYUVGRk5OTkpICSUYIEQwGA4EAsM+eqqper9fhcEDy6e7ubm9vj8fjYHK5XLW1tYgI7L4nhLh48eLp06fnz59fUVEhSRIw9ichIk3TWlpawuHwY489ZrfbgbGpIREBY/eUrusHDx5cuXIlAIyMjLS1tdXW1g4MDPT398+fP7+7u/vChQulpaUzZsyQZRkRIWFsbOzUqVPz588fHx83DKOoqGhsbOzIkSOrVq0KhUJnzpxZtWpVKBRqb29ftGjR9evXr127tmjRIrvd3t7eDgDl5eUtLS2FhYVpaWn79+9/+OGHCwsLe3t7e3p65s6dG4lEWltbV65cCQmRSOTChQt2u/3SpUtZWVlpaWl5eXmSJJ0/f350dJSIXC7X3LlzLRbLmTNnSkpK0tPTg8FgTk6OLMs7d+5cv369oih79uxZs2ZNNBo9cODAo48+Gg6HOzo6srOzR0ZGLl68WF5ePnv2bL/fT0SzZs2SZVmSJEgyQohgMBgIBIB99lRV9Xq9DocDkgwRnTx5cmhoSAgBACkpKUuXLrXZbMCYaWRk5MSJEzdv3pw/f77L5ZIkCRj79DRNu3LlypkzZ9asWVNcXCxJEjA2NSQiYOyeEkJcvXp1+vTpABCNRgcHB4uKisbGxsbHx3Nzc0Oh0MWLF0OhkNPpLCsrmzZtGkxy5cqV7u5ui8VSVVVlt9sB4Pz58729vVardcmSJVarNRqNXrx4cWhoKC0traysLCMjAwAGBwcBwO12X7x4MSMjIycn5+zZs0VFRRkZGQBw4cKF/v5+IcSKFSsURYEETdMGBgZUVY3FYpmZmaOjo2lpafF4XNM0t9sNAAMDA6qqZmRkXLt27erVq9FotKqqyuVyAYDf76+oqJAkqaOjo6ysTNO0zs7OWbNmxePx4eHh1NRURVEGBgZSUlJycnI6OzuvXLmCiOXl5QUFBZBkhBDBYDAQCAD77Kmq6vV6HQ4HJJlQKHT69OlwOAwAiqIsWrRo2rRpwFgCEfX39589ezYUChmGoWkaMPbp2e12XdfLysoWLlwoSRIw9n9CIgLG2H1MCBEMBgOBALDPnqqqXq/X4XBAkmlvbw8EArquS5JUVlY2ffp0RATGJhFCjIyMBAKB/v7+aDQKjH16mZmZJSUlhYWFsiwDY38MEhEwxu5jQohgMBgIBIB99lRV9Xq9DocDkkk8Hj958uTNmzcBwOPxLFiwQJZlYOxONBMRAWOfnizLVqtVkiRg7BNAIgLG2H1MCBEMBgOBALDPnqqqXq/X4XBAMunr62tra5uYmEhPT6+qqrLb7YgIjDHG2OcKiQgYY/cxIUQwGAwEAsA+e6qqer1eh8MBSUMIcf78+evXr1ut1nnz5rlcLkmSgDHGGPu8IREBY+w+JoQIBoOBQADYZ09VVa/X63A4IGmMjY21traGw+FZs2YVFRXJsgyMMcZYEkAiAsbYfUwIEQwGA4EAsM+eqqper9fhcEDS6OzsDAQC2dnZs2bNUlUVGGOMseSARASMsfuYECIYDAYCAWCfPVVVvV6vw+GA5KBp2pkzZwzDqKioSE1NRURgjDHGkgMSETDG7mNCiGAwGAgEgH32VFX1er0OhwOSw+DgYE9PT0FBQXZ2tiRJwBhjjCUNJCJgjN3HhBDBYDAQCAD77Kmq6vV6HQ4HJAEiCgQCFoslJyfHYrEAY4wxlkyQiIAxdh8jorGxsYGBAWCfPYvF4vF4bDYbJAFd10dGRhwOh81mA8YYYyzJIBEBY+z+put6PB4H9tlDRKvVKssyJAHDMBBRkiRgjDHGkg8SETDGGGOMMcZYMkEiAsYYY4wxxhhLJkhEwBhjjDHGGGPJBIkIGGOMMcYYYyyZIBEBY4wxxhhjjCUTJCJgjDHGGGOMsWSCRASMMcYYY4wxlkyQiIAxxhhjjDHGkgkSETDGGGOMMcZYMkEiAsYYY4wxxhhLJkhEwBhjjDHGGGPJBIkIGGOMMcYYYyyZIBEBY4wxxhhjjCUTJCJgjDHGGGOMsWSCRASMMcYYY4wxlkyQiIAxxhhjjDHGkgkSETDGGGOMMcZYMkEiAsYYY4wxxhhLJkhEwBhjjDHGGGPJBIkIGGOMMcYYYyyZIBEBY4wxxhhjjCUTJCJgjDHGGGOMsWSCRASMMcYYY4wxlkyQiIAxxhhjjDHGkgkSETDGGGOMMcZYMkEiAsYYY4wxxhhLJkhEwBhjjDHGGGPJBIkIGGOMMcYYYyyZIBEBY4wxxhhjjCUTJCJgjDHGGGOMsWSCRASMMcYYY4wxlkyQiIAxxhhjjDHGkgkSETDGGGOMMcZYMkEiAsYYY4wxxhhLJkhEwBhjjDHGGGPJBIkIGGPsLkSjUU3TIMFisdhsNmDsnoqbhBBEBJ8SItpsNovFgojAGGPsLwQSETDG2F3w+/3Dw8NgQsTs7OyKigpg7F4gopGRkZ6enhs3bkQiESEEESEiJCAi/DGSJNnt9qKiovz8fEVRgDHG2F8CJCJgjLG7sH///kAgACZZlgsLC1esWAGM3TVd169fv+73+69duzY2NkZEkICIcBtEhAREhElkWS4uLq6pqfF4PMAYY+wvARIRMMbYXdi/f38gEACTLMuFhYUrVqwAxu6OpmldXV0nT57s6+szDANug4hwG0SESRAREmw2W11d3YIFC4AxxthfAiQiYIyxu7B///5AIAAmWZYLCwtXrFgBjN0FTdMCgcCJEyd6e3thaogIt0FESEBESEDE2traxYsXS5IEjDHGkh4SETDG2F3Yv39/IBAAkyzLhYWFK1asAMb+VLquBwKBY8eO9fX1EREiwtQQEW6DiJCAiJBQW1tbU1MjSRIwxhhLekhEwBhjd6Gnp2dsbAwSUlNTCwoK4PNGRBMTE93d3SkpKQUFBYqiwOdnfHy8u7vb6XQWFBRIkgRsarqud3V1ffjhh/39/UQEJkSEqSEi3AYRIQERwVRbW1tTUyNJEjDGGEt6SETAGGO3CYfDY2NjRAQmVVXT0tLC4XAkEiEiMKmqmpWVFQwGw+EwmBAxJSXF5XLB503TtNbW1qtXr9rt9tmzZ3u9XvicEJHP5+vv77fb7QsWLCgoKAA2BcMwurq6jhw5MjAwIIRAREhARJgaIsJtEBEmQcTa2tqamhpJkoAxxljSQyICxhi7zZUrV86fPw8JOTk5c+bMuXz5ck9PjxACTG63u7a29uTJk729vWBCxNzc3AceeAA+b9FotKWlJRQKKYoyc+bM2tpa+JwYhvH666/rum61WmfPnr1w4UJgd2IYRldX1+HDhwcHB4UQYEJESEBEmBoiwm0QERIQsba2tqamRpIkYIwxlvSQiIAxxm7j9/uPHz9ORGAqLCysrq72+/2dnZ2apoEpLy9v9erVBw4cCAQCYJJlubCwcMWKFfB5i0QiO3fuDIfDsix7vd5ly5bB50TTtG3btgGAoiizZs1avHgxsNsQUSAQOHjw4NDQEBHBJIgIJkSEqSEi3AkiQkJdXV1NTY0kScAYYyzpIREBY4zd5sKFC8ePH4eEwsLC6upqv9/f2dmpaRqY8vLyVq9efeDAgUAgACZZlgsLC1esWAFT6+np0TQtPz/farXCZyYSiTQ3N09MTMiyXFpaunz5cvicaJq2bds2AFAUpaysrKamBtithBCdnZ379u0bHh4mIrgNIsIngIiSJFmtVrgVIoKp1iRJEjDGGEt6SETAGGO38fv9x48fJyIwFRYWVldX+/3+zs5OTdPAlJeXt3r16gMHDgQCATDJslxYWLhixQqYQiwW+/DDDwcHB6dPnz537lxVVeGzEYlEmpubJyYmZFkuLS1dvnw5fE40Tdu2bRsAKIpSVlZWU1MD7Fbd3d1vvvlmV1eXEALujtVqzc/Ph9sgIgDUmiRJAsYYY0kPiQgYY+w2Fy5cOH78OCQUFhZWV1f7/f7Ozk5N08CUl5e3evXqAwcOBAIBMMmyXFhYuGLFCpjC1atXT506NTY2ZrFYKisrKyoqLBYLfAYikUhzc/PExIQsy6WlpcuXL4fPiaZp27ZtAwBFUcrKympqaoBNcv369d27d1+6dOnmzZtw11RVLS4uhjtBxFqTJEnAGGMs6SERAWOM3WZkZOTGjRtEBCa73e5yuUZGRsbGxogITCkpKXl5eYODg2NjY2CSJMlut+fk5MAUjh071tHRoes6ANhstoceesjj8SAi3GuRSKS5uXliYkKW5dLS0uXLl8PnRNO0bdu2AYCiKGVlZTU1NcAS+vv7W1pabt68OTg4ePPmTbhrqqoWFRUhItxJXV1dbW2tJEnAkkA0Gu3u7g6Hw5Ikpaam5uXl2Ww2YIyxBCQiYIzdx4goHo/39fVBgqqqWVlZkUjkxo0bkOBwOLKyskKh0Pj4OBGBKSUlJS8vr7+/PxwOgwkRHQ5HVlZWKBQKh8OQkJ6enpGRoWmaz+e7du0aEQEAInq93rq6OovFAvdaJBJpbm6emJiQZbm0tHT58uXwOdE0bdu2bQCgKEpZWVlNTQ0w08TExDvvvHPjxg1d1wcGBm7evAl3TVXVoqIiAEBEuE1dXV1tba0kSfB/0jRt27ZtFy9eBICMjIw1a9YsXLhwcHBw7969586dgz/mscceq6qq+vd//3f4ZObPn//II48cOHCgtbVV13W4FSJaLJaCgoIFCxYsXLjQYrGAqb+/f8+ePW1tbXAnDoejuLi4rq7O6/XKsgx3YhjGmTNntm/fLoQAAKfT+cgjj9TU1MBtfvzjHw8MDIDpmWeeqaysjMVib7/99pkzZwDAZrNVV1evX78eEWGSvr6+d99998qVKwCwaNGiNWvWpKWlgWlsbOyDDz44ceJELBYTQqApPT198eLFdXV1aWlpwBhjAEhEwBi7jxHR0NDQ+++/DwnTpk2rqqoaHh4+ffq0EAJM+fn5CxcubG9vDwQCuq6DKTc39+GHH/b5fN3d3WCSZTk/P3/hwoVnzpwJBAKQMH/+/Llz54bD4aNHj16/fh0SrP+fPXgBivO+74X//T/Ps/vsld3lfr9JBiEQCBbEoqttLNmxLgbZaWLXTt8oDk3eetJp3+a8bd/mHOU9c9pz0jPNeZO+Ud7WSYcDTVLFMYmIgoLjxJJlpCDQBQSSuArEsgvL/bLs7nP5vzM7wwyMJFuxYx9U/T4fo/H55583mUz4UKanp0dHR6enp8PhMGNMluXk5OTs7Gyr1bqysnLq1KlgMCiKYk5Ozp49e3CXxcXFyailpSVVVS0WS2xsbEpKSlxcHGMMHyQUCo1HLS0taZomiqLL5UpNTU1JSZEkCasURWlsbAQgSVJeXl5lZSXuJRKJ3L59e2RkxOl0bt261Wq1Yj1FUfx+/8TExMLCQigUMpvNDocjKSkpISHBaDTiLqOjowMDA7Isb9++3Wq1AvD7/Xfu3JmdnQ2Hw4Ig2Gy2hISErKwsq9WK/xXOnTt348YNRVFUVZ2cnJyensZHJstyZmYmohhjWK+qqsrj8QiCgPelKMqJEyd6enoAuFyumpoaj8fj8/mam5s7OzvxQV588cWqqqqvfOUreDAVFRWHDx8+c+ZMe3u7qqpYjzEGQBRFo9GYk5Pzmc98JikpCYDX621qauru7sa9CIIgiqLBYKioqHj++edlWcZd5ubmTp8+/e6773LOATDGnnjiiUOHDlmtVqx3/Phxn8+HqLq6OrfbHQqFGhoaOjo6ADDG0tPTX3jhhS1btmCNO3funDx5sq+vD4DH46mtrXU6nQBmZma+853v+P1+VVU551jFGJNluaCg4MiRI6mpqSCEPPIY5xyEkEcY5zwQCJw+fRqrEhISduzYEQgELl26xDlHVHp6ekVFRW9v79DQkKIoiEpNTT1w4MDZs2eHh4cRJYpiRkZGeXl5Z2fn8PAwVpWVlW3bti0YDLa1tXm9XqxijJWXlzPGgsEgAKvVGhMTk5iYaDQa8b7m5+d7enpGR0cjkYiu6wAYY5xzURQFQcjMzCwoKHj77beDwaAoijk5OXv27MEaKysr/VHBYJBzrus6AMaYIAgAUlJSKisr7XY77kNRlJGRkd7e3vn5ec65ruuMMc65EOVwOEpLS1NTUwVBAKAoSmNjIwBJkvLy8iorK3GXmZmZrq4ur9erqipjbNeuXdnZ2aIoYtXt27c7OzuDwSDnXNd1ACxKEIS4uLjCwsKMjAzGGFYFg8Hz58/7fD7GmNvtdjqdPT09k5OTmqZxzhHFopxOZ1VVVUJCAj5ZKysrb7zxRjAY1DRNVVW/3z89PY2PTJblzMxMrGKMYY2qqiqPxyMIAt6XoignTpzo6ekB4HK5ampqPB6Pz+drbm7u7OzEB3nxxRerqqq+8pWv4MFUVFQcPnz4zJkz7e3tqqriPhhjoihu2bLlC1/4gsVi8Xq9TU1N3d3deF+SJD3xxBNHjx4VBAHrjYyMfO9735uYmMCqxx57rKamZvPmzVjv+PHjPp8PUXV1dW63OxQKNTQ0dHR0IEqSpLKyshdffNFisWDVnTt3Tp482dfXB8Dj8dTW1jqdTl3X//Ef//HGjRu6ruMujDFJksrLy1944QWbzQZCyKONcc5BCHmEcc4DgcDp06exKiEhYceOHYFAoL29HavS09MrKip6e3uHhoYURUFUamrqgQMHzp49Ozw8jChRFDMyMsrLyzs7O4eHh7GqrKxs27Ztmqa9++67IyMjWIMxhjUEQbBarWVlZTk5ObiP8fHxy5cvT01NAeCcYz3GGABZltUoURRzcnL27NmDVXNzc1evXh0dHdV1nXOOuzDGDAbDnj17MjMzcZdgMNjT03Pz5k1N0zjnuAtjTBAEt9tdUFAgCIKiKI2NjQAkScrLy6usrMQaqqoODQ319PTMz88D4JwD2LVrV25uriRJADRNa29v7+/v13Wdc471GGOcc5PJVBAlyzKiZmZm2traAoEAAIvFEolENE0DwDnHGowxWZa3bt1aUlKCT9bt27ffeeedSCSi67qmaePj41NTU/jIZFnOzMzEGowxrKqqqvJ4PIIg4H0pinLixImenh4ALperpqbG4/H4fL7m5ubOzk4AVqs1MzPT4XDgXqqqqjZt2vStb30LqzRNm5ubm56eBiCKosvlio2Nxar8/Pxdu3adOnWqvb1dVVUAr7zyyubNmwVBADA/P9/e3n7u3DkAjDGn01lTU+PxeLxeb1NTU3d3NwCn01laWpqRkQFA1/VAIHDlypWpqSld1wEIgvC1r30tNTUVayiK0tHRUV9fzzkXBIExpmmayWR6/vnn9+7di/WOHz/u8/kQVVdX53a7Q6FQQ0NDR0cHohhjDofjwIED1dXVWHXnzp2TJ0/29fUB8Hg8tbW1TqfzypUr9fX1oVCIcy5J0sGDB0tKShYWFi5cuHDp0iVd1xljiYmJhw4d2rFjBwghjzbGOQch5BHGOQ8EAqdPn8aqhISEHTt2BAKBS5cucc4RlZGRUV5e3tvbOzQ0pCgKolJTUw8cOPDOO+/cvn0bUaIoZmRklJeXd3Z2Dg8PY1VZWdm2bdsEQejs7Lx582YkEsF9MMYkScrLy9uxYwfuZWxsrLOzc2ZmBgBjDFGyLHPOw+EwYwwA5xyrRFHMycnZs2cPoubm5jo7O0dHRwEwxhBlMBgEQYhEIpxzRHHOBUHYtWvX5s2bscbKysr1KACMMUSJUaqq6rqOKM45Y+y5555zuVyKojQ2NgKQJCkvL6+yshKrFhYWurq6hoaGNE0DwBgDkJubW1paarfbAXDO33333eHhYV3XGWOIYowZjcZIJMI5B8A5ByBJ0pYtW4qLi2VZBjAzM9PW1hYIBLAGYwwAY8xkMkUiEU3TAJjN5tLS0ry8PHyyenp6Ojo6FEXRdV3TtPHx8cnJSXxksixnZmbiLowxAFVVVR6PRxAEvC9FUU6cONHT0wPA5XLV1NR4PB6fz9fc3NzZ2QkgIyOjtra2sLAQDyYYDLa2tra0tACwWq3V1dUHDx7EevX19e3t7aqqAnjttdcKCwsFQUCUoijf/e53r1+/DsBisezdu7e2ttbr9TY1NXV3dwNISUk5evRocXExVs3Ozp44cWJ0dJRzDuDgwYNHjhzBGrOzs2+++WZ7eztjLDEx0Wq1Dg0NAdizZ8/BgwddLhfWOH78uM/nQ1RdXZ3b7Q6FQg0NDR0dHVgjLy/vhRdeyMrKQtSdO3dOnjzZ19cHwOPx1NbWOp3OkydPnjt3TlEUxlhlZeXnP/95RM3Pzzc3N58/fx6ALMtPPfXU4cOHQQh5tDHOOQghjzDOeSAQOHPmDFYlJCS43e6pqamOjg6sSktLc7vdN27cGBoa0jQNUSkpKU899dS5c+dGRkYQJQhCenq62+2+fPnyyMgIVpWUlBQXFzPGxsfHL126NDs7yznHvTDGHA7H/v37bTYb7jI7O9vZ2Xnnzh0AgiCYzeaCgoJNmzaZzWbGWDAYHBwc7O7ujkQinHNEiaKYk5OzZ88eAKFQqKurq7e3l3POGJNlOScnJy8vz+VyMcbC4fDIyEh3d/fi4iKiZFn+1Kc+5XQ6EaWq6uDg4KVLlxRFYYxJkpSWlrZly5bExERRFMPh8OjoaE9Pz8LCgq7rjLEjR464XC5FURobGwFIkpSXl1dZWQlAUZSxsbFr167Nzs4CYIwBsNlsRUVFubm5RqMRUT09PVeuXFEUBYAoisnJyVu3bk1KSjIYDJFIZHx8vL+/f3x8nEeZTKbS0tL8/HzG2MzMTFtbWyAQwCrGWFxcXHFxcXp6uiiKnPPp6enx8XGTybR582ZBEPDJ6u3t7ezsVBRF13VN08bGxiYnJ/GRybKcmZmJe2GMVVVVeTweQRDwvhRFOXHiRE9PDwCXy1VTU+PxeHw+X3Nzc2dnJ4CMjIza2trCwkI8mGAw2Nra2tLSAsBqtVZXVx88eBDr1dfXt7e3q6oK4LXXXissLBQEAVGqqv7sZz9rbW0FYLFYHn/88eeee87r9TY1NXV3dwNISUk5evRocXEx1vjxj398/vz5UCgEoKys7I//+I+xxsjIyLe//e3FxUWz2VxRUWE2m1tbWznnGRkZzz//fEFBAdY4fvy4z+dDVF1dndvtDoVCDQ0NHR0dAFiUrusGg2Hv3r3PPfecLMsA7ty5c/Lkyb6+PgAej6e2ttbpdNbX17e3t6uqyhjbv3//0aNHGWMAdF1fWFgIBAIABEFwOBzx8fEghDzaGOcchJBHm6Io09PTWGU0Gm02WyQSWV5e5pwjSpZlm822srISCoV0XUeULMtOp3Nubi4cDiNKEASDwWC325eWlkKhEFZZrVaLxaLr+tTU1OXLl6empnRdx3qMMc65xWKprKzMzs7GXXRd7+rqun79uqIojLHY2FiPx5OQkMAYwxrT09Nvv/12MBjknAMQRTEnJ2fPnj2c85GRkUuXLi0tLTHGbDZbaWlpdna2KIpYIxgMnj171u/3A2CMpaamPvXUU4IgAJienm5ra5uammKMmc3mrVu35ufnG41GrBEKhbq6urxeb25ublFRkSiKiqI0NjYCkCQpLy+voqJiaWmpp6env79f0zQAjDFRFNPS0oqLi+Pi4hhjiFpYWGhtbV1aWuKci6JYXl6+detWrBcOh/v6+rq6uiKRCICkpKTy8vLExMSZmZm2trZAIIAoQRDy8vJ27NghiiI2ht7e3suXL6uqquu6pml37tyZmJjARybLcm7otS8AACAASURBVGZmJu5j586dHo9HEAS8L0VRTpw40dPTA8DlctXU1Hg8Hp/P19zc3NnZCSAjI+O5554rKCjAXURRZIxhvWAw2Nra2tLSAsBqtVZXVx88eBDr1dfXt7e3q6oK4E/+5E+2bt0qiiIAzvnKysp3v/vdvr4+xlhsbGxtbW1FRYXX621qauru7gaQkpJy9OjR4uJirOKc/9u//VtbW1s4HAZQXl7+xS9+EavC4XBbW9uPfvQjAKmpqTU1NaFQ6Mc//vHi4qIkSUePHt23b58kSVh1/Phxn8+HqLq6OrfbHQqFGhoaOjo6AFit1vj4+NHRUc55UlLS4cOHy8vLGWN37tw5efJkX18fAI/HU1tb63Q6m5qa3n77bUVRGGMxMTGvvPLKY489xhgTBEEURUEQQAghqxjnHISQRxjnXFGUhYUF/F7puq5EIUpRlLm5Ob/fPzs7q2ka1mCMARBFkTFmt9srKipSU1NxL1NTU52dnePj4wCsVuvOnTvT0tIYY1iPc+7z+d5++21VVQGIopiTk7Nnz57l5eXu7u4bN24AMBqNJSUl+fn5BoMBd1lcXPzFL34RDAYBmEymxx9/PCUlRVGUgYGBixcvApAkKT8/f/v27UajER9EUZTGxkYAkiRt2rQpPT396tWr09PTABhjgiBYLJb8/PzHHnvMZDJhjUuXLt26dUtRFMZYQUFBZWUl7mV5efn69es3btzgnIui6Ha7CwoK5ubm2traAoEAAMZYWlravn37jEYjNoybN29evnxZVVUtamxszOfz4SOTZTkzMxP3sXPnTo/HIwgC3peiKCdOnOjp6QHgcrlqamo8Ho/P52tubu7s7ATgcrmKi4uTk5Nxl5ycnKysLEEQsEYwGGxtbW1paQFgtVqrq6sPHjyI9err69vb21VVBfAHf/AHubm5giAAmJmZaW9vv3LlCgCTyVRUVPTyyy+bTCav19vU1NTd3Q0gJSXlyJEj27ZtQxTnPBAIfP/73/d6vZxzAIcOHTp8+DBWzc7ONjY2Xr9+nTFWVFT08ssvz8/P/+xnP+vp6QGwY8eOQ4cOJSUlYdXx48d9Ph+i6urq3G53KBRqaGjo6OgAkJGRsXv37rfeemtqagpAWVlZTU1NUlLSnTt3Tp482dfXB8Dj8dTW1jqdzt7e3tdffz0YDHLOATDGbDZbenp6RkZGTk5OdnZ2TEyMJEkghBCAcc5BCHmEaZrm9XrffvttfOIEQTCbzbIsGwwGl8uVkJCQmZlpNBpxHwMDA1euXFlaWmKMFRcXb9261WQy4V40TWtvb7958yYAURRzcnL27NkzOTl56dKlyclJxlhWVlZpaanT6cR93Lp168KFC5xzg8GwZcuW8vLypaWljo6O4eFhAImJiRUVFYmJiXgAiqI0NjYCEATBZDJFIhFVVRElSVJycnJRUVFycjJjDGvout7S0hIIBDjnRqPx8OHDJpMJ9zE+Pt7Z2bmwsADgscce2759eyQSaWtrCwQCAERRfPrppxMTExlj2DBu3rx59epVVVW1qNHRUZ/Ph49MluXMzEzcC2OsqqrK4/EIgoD3pSjKiRMnenp6ALhcrpqaGo/H4/P5mpubOzs78b4OHTr0zDPPGAwGrBEMBltbW1taWgBYrdbq6uqDBw9ivfr6+vb2dlVVcS+MMaPR6Ha7a2pqHA4HAK/X29TU1N3dDSAmJmbbtm0pKSmICoVCV65c8fl8uq4DMJlMX/3qV9PT0xHFOR8ZGfmHf/iHcDhsMpn27t37/PPPB4PBM2fOvPXWW7qux8fHv/jii0VFRVh1/Phxn8+HqLq6OrfbHQqFGhoaOjo6AGRlZX3mM5/p6+s7ffq0oigWi+Xpp59+8sknJyYmTp482dfXB8Dj8dTW1jqdTl3X//mf//n69euRSATrMcYSEhKqq6t37NhhsVhACHnkMc45CCGPMF3XvV7vr371K3zinE7ngQMHrFYrHsy1a9euXLnCOTeZTLt27crMzMR9cM4nJibOnDnDORdFMScnZ8+ePXfu3HnvvfdWVlZEUSwtLS0qKmKM4T4URfnBD36g67ogCKmpqfv375+dnT179uzs7KwgCJs2bfJ4PJIk4QEoitLY2Ih7SUxMdLvdSUlJjDGst7i42NraurCwAMBqtWZmZuL+IpFIIBBYWFgAkJqa6na7BUFoa2sLBAIAkpKS9u3bZ7VasZHcunXr2rVriqLouq5p2sjIyPj4OD4yWZYzMjIAMMawniRJu3fvLi8vxwdRFOXEiRM9PT0AXC5XTU2Nx+Px+XzNzc2dnZ14X4cOHXrmmWcMBgPWCAaDra2tLS0tACwWS3V19aFDh7BefX19e3u7qqoARFHEKs65rusAjEbj9u3bn3322aSkJEEQvF5vU1NTd3c33pfFYtm/f/8zzzwjCAKiwuHwr3/965/+9KcAkpKSamtrS0tLAfz2t79tamqanZ0FUFNT88QTT5hMJkQdP37c5/Mhqq6uzu12h0KhhoaGjo4OAFlZWceOHVNVtampqaenh3OenZ195MgRm832xhtv9PX1AfB4PLW1tU6nE8DS0tLJkydv3bq1vLysqirnHGvYbLa9e/c+/fTTJpMJhJBHG+OcgxDyCOOc+/3+M2fO4JMlCML27dsLCgqMRiMegKqq165d6+rqAhAfH19ZWZmYmIj7W15efvPNN1VVFUUxNzd39+7dw8PD58+fV1XVYrG43e7NmzfjfTU1Nc3NzTHG4uLiDh8+PDU19fbbbweDQYPBUFBQ4Ha78WAURWlsbMQqxpggCLquc84NBsPmzZuLi4stFgvWCwQCZ8+eXVxcxO8oISGhvLzcaDS2tbUFAgEA+fn5brdblmVsJH19fV1dXYqi6Lquadrw8PD4+Dg+MlmWMzIyADDGsF5MTMzu3bu3bt2KD6IoyokTJ3p6egC4XK6amhqPx+Pz+Zqbmzs7OwFIkmSz2YxGI+7y+OOP79u3T5IkrBEMBltbW1taWgBYrdbq6uqDBw9ivfr6+vb2dlVVAWRnZ1utVsYYgFAoND8/Pzs7q6qqIAg5OTkvvfRSenq61+ttamrq7u7GfQiCEBsb6/F4nnnmGYPBgFVzc3P/9E//NDg4yBjLzc196aWXYmNjAYyOjv7iF7+4desWgKKioqNHj6alpSHq+PHjPp8PUXV1dW63OxQKNTQ0dHR0AMjKyjp27FhCQsLly5ffeOONubk5QRB27dq1bdu2X/3qV319fQA8Hk9tba3T6cSq69evX7p0yev1hsNhRVHC4fDKygrnHEBKSsqRI0fKyspACHm0Mc45CCGPMM75zMxMc3Mz5xyfIKvV+uSTT8bFxTHG8ABUVb1y5cr169cBJCYmVlRUJCYm4v6CweDPfvazUCgkSVJubu6uXbuGh4fPnj3LObfZbGVlZZs2bcL7OnXq1PT0NGMsLi7u8OHDU1NTb731VigUMhqNhYWF27dvx4NRFKWxsRFRgiDYoubm5oLBIACTyVQQJcsy1piYmHj33XcXFxfxu2CMJScnu91uURTb2toCgQCAoqKi4uJiWZaxkfT393d3d6uqqmmaqqq3b98eGxvDRybLckZGBlYxxhDFGMvOzt69e3dSUhI+iKIo3/nOd3p7ewG4XK6amhqPx+Pz+Zqbmzs7OwEkJSU9+eSTubm5uIvD4YiJiWGMYY1gMNja2trS0gLAarVWV1cfPHgQ69XX17e3t6uqCuC1114rLCwUBAFRQ0NDP/nJTwYGBgDIsrxjx46XXnrJ5/M1NTV1d3cDMBgMDofDbDZHIpG5ublwOAzAaDR+/vOfLy4uliQJq3RdHxoa+uY3v6mqKmMsJSWlrKwMUeFw+NatW6OjowBsNtsrr7xSXFwsCAKA48eP+3w+RNXV1bnd7lAo1NDQ0NHRASArK+vYsWPJycmzs7NvvfXWuXPnFEVxOBylpaUjIyPDw8MAPB5PbW2t0+nEeqqqBgIBn883ODh4+fLlmZkZAJIk7du37+jRo5IkgRDyCGOccxBCHm0LCwunT58OhUL4pDDGioqKtm3bJssyHgzn/Nq1a1evXuWc2+32qqqqtLQ03AfnfG5u7tSpU7quG43GgoKCsrKykZGR9957LxwOG43G0tLSgoICxhjuQ9O0N954IxgMCoKQmpq6f//+mZmZd955Z35+XhTFvLy8HTt2CIKAB6AoSmNjIwDGmNPp3L59e1JS0uDg4PXr11dWVgDY7fZt27bl5OQYjUasmpub+/Wvfz0/Pw9AkqSYmBg8AEEQcnNzN23aFAwGL1y4MDk5CaCoqKi4uFiWZWwk/f39PT09iqLouq6q6tDQ0NjYGD4yWZYzMjKwBmMMgMViKS8vd7vdoijig0QikW9+85tDQ0MAYmNja2pqKisrfT5fc3NzZ2cngIyMjNra2sLCQjyYYDDY2tra0tICwGq1VldXHzx4EOvV19e3t7erqgrgtddeKywsFAQBUZzz3t7eb33rW4jatGnTl7/85YWFhaampu7ubgBJSUnPPvvs1q1bA4HAqVOn+vr6dF0HcODAgYMHD5pMJqwKh8OnT5/+5S9/iQ/yqU99qrq62m63Azh+/LjP50NUXV2d2+0OhUINDQ0dHR0AsrKyjh07lpycDKCvr++nP/3p0NAQ59zlckUikeXlZQAej6e2ttZut09OTmqaBoAxlpqayhhD1MrKyrlz5958801ElZeXf/rTn3Y6nSCEPMIY5xyEkEfb8vLy+fPnx8fH8UlxOBx79+6Ni4tjjOGB3bp16/Lly6FQSBTFioqKxx57TJIk3IumaTdu3Lh06RIAi8Wyffv2/Px8v99/8eLF2dlZAFu2bNm+fbvZbMZ9+P3+lpYWAJIk5eXlVVZWLiwsXLx40ev1AkhPT6+oqHA6nXgAiqI0NjYCkCQpLy+vsrISwPLycm9vb19fXyQSARAXF1dcXJyeni5JEqLC4fAvf/nLmZkZzrnVat27d6/RaMT7YowZDAaz2SyK4szMzIULFyYnJwEUFRUVFxfLsoyNZGBgoLe3V1EUXddVVR0cHLxz5w4+MlmW09PTGWNYw2Aw5OfnV1ZWxsbG4oNomjY6Ovrtb397eXkZQHx8/Gc/+9lt27b5fL7m5ubOzk4AGRkZtbW1hYWFeDDBYLC1tbWlpQWA1Wqtrq4+ePAg1quvr29vb1dVFcBrr71WWFgoCAKiOOdDQ0Pf+MY3EJWdnX3s2DFVVZuamrq7uwGkpKTU1taWlJQA+M1vfnPmzJm5uTkAVqv1y1/+8ubNmxljiJqfn//mN7/p8/kYY6IoyrKM9SKRiKIoAHJzc1966aWMjAwAx48f9/l8iKqrq3O73aFQqKGhoaOjA0BWVtaxY8eSk5MBRCKRd999t6WlZXFxEWt4PJ7a2lpJkr73ve8tLi5yzhljX/rSl+Lj4xGlaVpXV9d3v/tdRJWVlX3605+OjY0FIeQRxjjnIIQ82iKRyPXr169du4ZPhCiK27dvz8/Pl2UZvwuv13vlypVAIAAgLS2trKwsPj4e9zI/P3/27Nnp6WnGmMvl2r17d1xc3MLCwpUrV4aHhznnsbGxJSUlGRkZoijiLuFw+L333hsZGWGMGY3GnTt3Zmdnh0Kh3t7erq4uzrnVai0sLMzLyzMYDLgXTdNEUUSUoiiNjY0AJEnKy8urrKxE1NzcXFdX18jIiKqqAJKTk0tKSpKSkkRRRNT58+eHh4dVVQWwe/fuzZs3M8ZwF845ohhjWDUzM3PhwoXJyUkARUVFxcXFsixjIxkaGurp6VFVVdd1VVUHBwdHRkbwkcmynJ6eDoAxhiiDwZCdnb1jx47U1FTch67rQ0NDqqoCWFhYOH/+/K1btwAwxrKzs48dO5aYmOjz+Zqbmzs7OwEkJyc/+eSTmzZtwr2Yzea4uDisEQwGW1tbW1paAFit1urq6oMHD2K9+vr69vZ2VVUBfPGLX8zLyxNFEVELCwu//vWvz507B4Ax9thjj7366qtLS0tNTU3d3d0AUlJSamtrS0pKAMzNzf3gBz/o6elRVRVAWVnZyy+/bLVaAXDOe3p6vv3tbwOQJCkzMzM/Px/rDQ0NDQ4OqqpqNBo/97nPlZaWSpJ0/Phxn8+HqLq6OrfbHQqFGhoaOjo6AGRlZR07diw5ORlRExMTp0+f7uzsVFUVqzweT21trd1u/8Y3vjE6OqrrOmNs//79u3fvttlsuq7PzMy0tbW98847ACRJ2rNnT21trSzLIIQ8whjnHISQRxvn3Ov1/uY3v1FVFR8zxlhycnJlZaXL5cLvaHl5uaurq7+/X9M0URTz8vLy8/MdDocgCFhjdna2p6env78fgCRJWVlZO3fulCRJ07T+/v7Lly+Hw2HGWHp6+tatWxMTEyVJwhrLy8v9/f3Xrl3TdV0QhPj4+CeffNJsNnPOvV5vW1vb8vIyYyw+Pn7r1q3p6elGoxFrRCKR6enp5eVlu92emJjIGFMUpbGxEYAkSXl5eZWVlYjinE9NTV27dm18fFzTNMZYZmZmcXFxXFwcYwzA6OjoxYsXg8Eg59xut3s8nsTERKPRiDUURZmZmVleXrZarXFxcZIkIWpmZubChQuTk5MAioqKiouLZVnGKs75wsKCrusALBaLLMv4xA0NDd24cUNVVS2qv79/ZGSEMWaxWGJiYnAfKysrS0tLqqriPmRZTk9PRxRjzGAwZGVlVVRUpKen4/5CodDf//3fz8/PA1hcXEQUY8xisTzxxBOHDh1ijPl8vubm5s7OTgAmkyk+Pt5qteJe8vLynn32WUEQsCoYDLa2tra0tACwWq3V1dUHDx7EevX19e3t7aqqAqisrExISGCMIcrr9XZ1damqCsBkMu3evfuFF14YHx9vamrq7u4GkJKSUltbW1JSgqhLly799Kc/nZ6e5pwbDIZjx46VlpYyxlRV/dd//de2tjYADofj2Wefffzxx7HexYsXf/7znwcCAQD79u179tlnnU7n8ePHfT4fourq6txudygUamho6OjoAJCVlXXs2LHk5GREcc6vXr166tSp8fFxrKqqqqqpqXE6nWfOnPnFL34RiUQASJK0bdu2tLQ0VVW9Xu+tW7fC4TBjLDEx8dChQzt27AAh5NHGOOcghDzyFhcX33333YmJCXzMbDZbeXl5ZmamKIr43Y2NjV25cmV6eppzLklSZmZmVlZWTEyMLMsAIpHI4uJif3//6OgoAMaYy+UqLy9PS0tD1Ozs7JUrV8bGxjRNY4wlJydv2rQpNjbWZDIBUFV1eXl5dHS0v79f0zQAJpOpvLx88+bNiFpeXu7t7b1165aiKIwxl8uVm5ublJRkNpsFQdA0LRQKBQKBmzdvLi4uxsbG7t+/32w2K4rS2NgIQJKkvLy8yspKrOKc+3y+q1evBgIBXddFUczNzd22bVtMTAxjTFXV9vb2wcFBVVUB2O32goKCpKQks9ksCIKiKKFQKBAIDA4Ozs3NJScnl5aWJiQkIGpmZubChQuTk5MAioqKiouLZVnGqunpab/fHwwGRVGMi4tLS0uTJAmfrOHh4Zs3b6qqquu6qqp9fX23b982GAyFhYUejwf3MTAwcOnSpfn5edyHLMvp6emIMhqNmZmZFRUVGRkZeF/BYPDrX//63NwcVjHGzGZzSUnJc88953K5APh8vubm5s7OTnwQt9v96quvCoKAVcFgsLW1taWlBYDFYqmurj506BDWq6+vb29vV1UV98EYMxgM+fn5R44cyczM9Hq9TU1N3d3dAFJSUmpra0tKShC1srLywx/+8MqVK5FIBEB2dvaXv/xlp9M5Nzf3d3/3d3Nzc4yx1NTUY8eOpaenYz2v1/uTn/ykp6cHQEpKyuc///nMzMyvf/3rPp8PUXV1dW63OxQKNTQ0dHR0AMjKyjp27FhycjJWBYPB1tbWs2fPBoNBRO3cufO5555zOp2zs7ONjY03btzQNA13YYzZ7faqqqqnnnoqJiYGhJBHG+OcgxDyyItEIv39/Z2dnZqm4WMjy3JBQcGWLVvMZjM+FEVR+vr6ent7l5eXOecAbDZbXFyc2WxmjAWDwYmJiVAoBIAxZjabi4qKCgoKBEFAlK7rY2NjV69enZ2d1XUdgMlkcrlcMTExoiiurKzMzMwsLCxwzgGIopifn+92uyVJQhTnfHp6+sqVKz6fT9M0AAaDweVyORwOg8EQiUTm5+dnZ2c1TeOcS5J05MgRh8OhKEpjYyMASZLy8vIqKyuxhqZpo6OjXV1ds7OznHODwZCfn19UVGQ2mwHMzs7+9re/9fv9nHMWlZSU5HQ6BUEIh8Pz8/Nzc3OKogBwOp2lpaXZ2dmImpmZuXDhwuTkJICioqLi4mJZlhHFOe/s7ExMTBwbGzNHJSQkxMXF4ZM1PDzc19enqqqu66qq3rp1a3h42GQy7du37+jRo7iPS5cunTp1anJyEvchy3J6ejoAg8GQmZm5Y8eOjIwMfJBgMPj1r399bm4OgCRJZrM5Li4uLy9vz549iYmJiPL5fM3NzZ2dnfggbrf71VdfFQQBq4LBYGtra0tLCwCLxVJdXX3o0CGsV19f397erqoq1mOMGY1Gm80WGxublpbmdrvz8vIAeL3epqam7u5uACkpKbW1tSUlJVh148aNH/3oRxMTE5xzAEePHn366acvXbr0+uuvA5AkqaioqK6uThRFrKcoSlNT07lz5xRFEQTh5Zdfrqio+Nu//Vufz4eouro6t9sdCoUaGho6OjoAZGVlHTt2LDk5GWuMjIycOnXqxo0bmqYB8Hg8tbW1TqcTwMjIyFtvvTU2NhYIBFRVRRRjTJblpKSkbdu2VVZWJiYmghDyyGOccxBCCDA3N3fx4kWfz4ePhyRJ2dnZxcXFDocDH0EwGOzv7+/r61teXuac414YYxaLJT8/v6CgwGg0Yo1IJDI6Otrb2zs7O6vrOu5DkqTc3NzS0lKLxYI1NE2bmJi4fv365OSkoii4D0mSMjMzq6qqjEajoiiNjY0AJEnKz8/fsWMH1otEIkNDQ9evX19aWuKcm83mXbt2paenM8YA+Hy+rq4un8/HOcd9GI3GrKysoqIip9OJqJmZmQsXLkxOTgIoKioqLi6WZRlRnPPz589v2bKlv79fEITk5GSDwZCWloZP1sjISF9fn6qqWtTNmzeHhoYMBsPmzZu3b9+O+xgZGenu7l5cXMR9yLKcnp4uSVJWVlZlZWV6ejoegKIop0+fXllZAWA0Gu12e1pa2ubNm2VZxqqFhYXu7u7R0VF8kMzMzJ07dzLGsCoSidy8ebOnpweA0WjcsmVLYWEh1vvtb387MjKiaRrWY4xZLBan05kWZTKZEDU3N9fV1eX1egE4HI6SkpK0tDSsUlX13Llzk5OTnHMAsbGxTz/99JUrV27evAnAYDBs2rSptLQU93Lz5s0bN26EQiEAW7Zs2bp169tvvz0/P4+o3bt3Z2RkKIpy6dKlkZERALGxsVVVVTExMVhD1/Wurq7+/n5VVQHk5OSUlJSYzWZEzc3NDQwMDA8Pz83NhcNhxpjZbI6Njd28efOmTZvMZjMIIQRgnHMQQgigadro6Ojly5cXFhbw+yZJUlpa2rZt2+Lj4xlj+GhWVlZu3749MjIyNTWlqirnHGtIkpSYmJiRkfHYY48ZDAbcJRKJeL3e4eHhycnJUCjEOccaoii6XK7U1NSCggKLxYK76Lo+PT09ODjo9XoXFxc551iDMeZwOFJTU7ds2RITE8MYU1X1xz/+cSgUkmW5sLCwpKQEd1lZWbl161ZfX9/y8rIgCLt3787OzhZFEVETExN9fX0TExOLi4tYTxRFm82Wlpa2adOm+Ph4rFpcXHzvvff8fj/nvLy8fMuWLQaDAVGc81u3biFqaWnJbDZnZmba7XZ8skZGRvr7+7VVN27cGBwcxEcmy3J2dnZmZmZlZWVGRgbIBqbr+sLCwsrKCgCbzWa1WgVBACGErGKccxBCSNTKykpfX19vb28oFMLvjyRJKSkp27ZtS0xMZIzh90HTtOnpaZ/PNzs7GwwGVVUFIEmS1Wp1Op3JyckJCQmCIOA+dF2fn5/3+/1TU1PLy8uKonDORVE0m80OhyMpKSklJUUURdzf8vKy3++fnJxcXFyMRCKcc8aYyWRyOBwJCQkpKSlGo5ExBoBz3tXV5fV67XZ7QUFBfHw87mVpaWlgYGB8fNxqtW7bts3lcjHGsGplZWVsbGx6enp+fl5RFE3TRFE0mUwxMTEJCQmJiYlWqxVraJrW19d3584do9FYWFgYHx/PGMOqYDDo9/tlWdY0TZKklJQUxhg+WaOjowMDA6qq6rquqmpvb+/g4CA+MrPZvGvXrp07d6alpYEQQsjDjHHOQQghq5aWlnp6eoaHh1dWVvD7IMtyWlpaQUFBfHy8IAj4vdJ1fXFxMRgMapoGQJIkq9Vqt9vxYDjnwWBweXlZURTOuSAIFovFbreLoogHEwqFlpaWIpGIruuCIJjNZpvNZjAYsJ6iKNPT02az2eFw4P5WVlbm5ubMZrPdbhdFEXcJh8Pz8/OKonDOBUEwmUxWq1WWZdxLJBJZWFgwGAx2u10QBKynadrKyorBYJBlGf8rjI2NDQ4OKoqi67qmaX6/f3JyEoCu6wD4GgB0XQfAVwHg98IY27Rp06c+9an09HQQQgh5yDHOOQghZBXnfHFxsb+/f2hoaGlpCR8BYywmJiYnJyc7O9vpdDLGQMiqsbGxoaEhLUrXdVVVFUXhnOu6zjnXdV3TNM65vgbnXNd1TdM457quc8719bKysjweT2ZmJgghhDz8GOcchBCy3vLy8ujo6ODg4OzsrKqq+B0xxmRZTk1NzcrKSkpKMpvNIGQ9r9c7PDysqqp+F865ruuapumrOOf6Gpqmcc71NTjnGRkZVVVVycnJIIQQ8u8C45yDEELuEolEpqamvFELCwuapuEBCIJgMpmSkpJSU1MT9gvpbwAAIABJREFUExMdDgdjDITcZWZm5tatW4qi6PfCOdc0TV+Dc66voWka51yPYozl5ORs3749KSkJhBBC/r1gnHMQQsi9cM5DodDs7Ozc3Nzs7Ozc3NzCwkI4HOacYw3GmCAIFovF4XDERTmdTpvNJooiCLkPRVGGh4cDgYCmafqD4Zzruq5pGudc0zRd1w0Gg8PhyMrKysnJcTqdIIQQ8u8I45yDEELel6ZpwWBwZWUlHA6rqqooCudc13XOOWPMYDCIoijLsslkslgsJpMJhDyAYDC4tLSkKArnHABfDwDnHICu6wD4KgA8ShRFo9FoNptdLpfRaAQhhJB/XxjnHIQQ8rvQdZ1HMcYACILAGAMhvzvOua7ruA/OOe6DMSYIAmMMhBBC/p1inHMQQgghhBBCyEbCOOcghBBCCCGEkI2Ecc5BCCGEEEIIIRsJ45yDEEIIIYQQQjYSxjkHIYQQQgghhGwkjHMOQgghhBBCCNlIGOcchBBCCCGEELKRMM45CCGEEEIIIWQjYZxzEEIIIYQQQshGwjjnIIQQQgghhJCNhHHOQQghhBBCCCEbCeOcgxBCCCGEEEI2EsY5ByGEEEIIIYRsJIxzDkIIIYQQQgjZSBjnHIQQQgghhBCykTDOOQghhBBCCCFkI2GccxBCCCGEEELIRsI45yCEEEIIIYSQjYRxzkEIIYQQQgghGwnjnIMQQgghhBBCNhLGOQchhBBCCCGEbCSMcw5CCCGEEEII2UgY5xyEEEIIIYQQspEwzjkIIYQQQgghZCNhnHMQQgghhBBCyEbCOOcghBBCCCGEkI2Ecc5BCCGEEEIIIRsJ45yDEEIIIYQQQjYSxjkHIYQQQgghhGwkjHMOQgghhBBCCNlIGOcchBBCCCGEELKRMM45CCGEEEIIIWQjYZxzEEIIIYQQQshGwjjnIIQQQgghhJCNhHHOQQghhBBCCCEbCeOcgxBCCCGEEEI2EsY5ByGEEEIIIYRsJIxzDkIIeUgEl+aCy4u6roEQ8rtggGQw2h3xksEIQgh5GDDOOQgh5GGwvDi7MDdllM2iZAAh5HfCeTgcBOcJyVmCKIEQQjY8xjkHIYQ8DCZ9w5JBjo1PBSHkd6epyqTvdowz3mp3gRBCNjzGOQchhDwM/GMDVrvL7ogDIeRD8Y8NWmyOGGc8CCFkw2OccxBCyMPAPzZgtbvsjjgQQj4U/9igxeaIccaDEEI2PMY5ByGEPAz8YwNWu8vuiAMh5EPxjw1abI4YZzwIIWTDY5xzEELIw8A/NmC1u+yOOBBCPhT/2KDF5ohxxoMQQjY8xjkHIYQ8DPxjA1a7y+6IAyHkQ/GPDVpsjhhnPAghZMNjnHMQQsjDwD82YLW77I44EEI+FP/YoMXmiHHGgxBCNjzGOQchhDwM/GMDVrvL7ogDIeRD8Y8NWmyOGGc8CCFkw2OccxBCyMPAPzZgtbvsjjgQQj4U/9igxeaIccaDEEI2PMY5ByGEPAz8YwNWu8vuiAMh5EPxjw1abI4YZzwIIWTDY5xzEELIw8A/NmC1u+yOOBBCPhT/2KDF5ohxxoMQQjY8xjkHIYQ8DPxjA1a7y+6IAyHkQ/GPDVpsjhhnPAghZMNjnHMQQsjDwD82YLW77I44EEI+FP/YoMXmiHHGgxBCNjzGOQchhDwM/GMDVrvL7ogDIeRD8Y8NWmyOGGc8CCFkw2OccxBCyMPAPzZgtbvsjjgQQj4U/9igxeaIccaDEEI2PMY5ByGEPAz8YwNWu8vuiAMh5EPxjw1abI4YZzwIIWTDY5xzEELIw8A/NmC1u+yOOBBCPhT/2KDF5ohxxoMQQjY8xjkHIYQ8DPxjA1a7y+6IAyHkQ/GPDVpsjhhnPAghZMNjnHMQQsjDwD82YLW77I44EEI+FP/YoMXmiHHGgxBCNjzGOQchhDwM/GMDVrvL7ogDIeRD8Y8NWmyOGGc8CCFkw2OccxBCyMPAPzZgtbvsjjh8zPRIePn2cMjvC08FDA6nOTVNTkqWY+N0JRIOTIZ84yG/H+ByUrIlPVOOT2CShAcWDodP/eync7Ozy8HllZUVcMiy0elyZWZmFRVtS0hMFEURhHw8/GODFpsjxhkPQgjZ8BjnHIQQ8jDwjw1Y7S67Iw4fJ11VA++8PXulY/piW2R22uBwxRQUJj75lKOgSF1emr5wPvDeuyt3Rriu23I3u3Z4EnbtseU+xiQJD2Z+fv7gpw4sLiyEQuFwOATAaDTabLak5JRNmza98AefqaysNJstIORj4B8btNgcMc54EELIhsc45yCEkIeBf2zAanfZHXH4+HA+e+3K6A8bZjvbl28PAWCCYHTFxu6oStz3ZHhmyveL5qXBfm1lBYBktdnzC1KePZz67BE5MQkPZmZmpqykSBCE7aVlOyo9jDElEhm9M/LbixcXFxZKy9x/+1//W15ePmMMhPy++ccGLTZHjDMehBCy4THOOQgh5GHgHxuw2l12Rxw+NlzXb9e/PvKv9cu3h7CGKTEpsfpAZGpq4je/4qqCVZLNFr/78U11f+Is3o4HMzMzU1ZSJEnSsS98se5LXxIEQdO0ubm5ltM///73Xl9aWvrv//A/Djz9jMViASG/b/6xQYvNEeOMByGEbHiMcw5CCHkY+McGrHaX3RGHjw3XtO7/66t33vgh1hNMJkt6phZaWRm7g/Vi8gvy/o+/SnpyPx7MzMxMWUmRJEmvfeVPv/KnfyYIAqKuX+/+41ePeb3ev/6b//iZz37W4XAiamJioqH+X07//Ode75jT6fTs3PXyK5/bsaMSaywtLb13/t0f/eAH3d1dy8tLTpdr567dL7/yudLSMgChUOjq5cv19f/Scak9GAympaU//cwzL3z6M1nZ2VjVeubMr3/9q/6+/vHxsZWVFaPRmLdlyx/+4SuxcXE/ffMnv714cWZm2m63F2/f/uqrdaVlbqwxNTX1ox80/ry5eWho0GazlVdUvvJHf7Rnz17cx62bN19//Z86L7UHAgFVVWNjY3ft2fvyK58rLi7Bfdy+ffvNn/z4ly2/GBsbY4ylpaUdePqZ2udfyM3dBOB/1v/LtatXh4aGxu6MKopit8cUFhX9wWderPR4rFYr1picmGh68yenf35qeHhYkgxFRdte/tzndu/Za7VaERUMBv+/E9+5cfPGyPDtmZkpRVGsVmt+QcGnP/2ZXbv3xMTEIGphYeHdd8821v/P3t7r4XDY4XAUl2w/9uoXq6p2IkpRlO6ua9///vfaL15YWlpKTk7Zv//AZ1/6w5zcXKzquNR+7uw7165eHR0dWVhYkGXTps2b/9PX/+/Nmx/Dx8Y/NmixOWKc8SCEkA2Pcc5BCCEPA//YgNXusjvi8LHhmnb9+F/dOfkDrmlYjwkCAK7rWC+moHDLV/86Ye+TeDAzMzNlJUWSJL32lT/9yp/+mSAIiDrTcvo/fu1vJicmvvX/fmf//qfNZjOAy5c7//KrfzEw0M+jADDGUlJS//TP/vwzn30RURMTE//yvde/9/o/qarKOUfU9u2lf/4XX9277/HFxcV/+9EP//s3/ms4HOacA2BRhYVFX/0//3LvvscR9Vd/+R9+9tMmJaJomqrrOmPMYDTGxMSIori4sBAOhzVNE0TRaDRm5+R88398q6BgK6J6rnd/7W/++trVq7quc84BMMZcrtj//U9ee7Xuj3Ev7e2//U9f+5sbvT1YIyc395v/z7e3by/FXQYHB//Lf/76ubPvaJrGOUfUU/sP/PlffHXr1kIAngp3IDDJOdd1HVGCIBiNxi+8+sU/+t+OJSYlIaq7u+u//d3fXrzQpmka5xwAY0wUxWNf+GLdl74UH58AYHp6+ol9uyPhiKqqmqZyzgVBMBqNDqfzP/zlXx048Izdbp+fn2t6882/+y//OfL/twcnQE2dbcOA7+cQCSQ5OQmJKGhbocguIhQXBCuL3bUuFdSquFtFQKqyKiCI4oZb1W6igKitG7hLBautXV3oq7YiCahEPRIIQgCBkDz/P8w4o+P3ftPyTR0yc19XezulFDqxLLtx05a33n4HAJ48eXLo0MHM9LS2tjZKKQCQTk5O/eMTk0JCR0GnrNWr9u8raG1t7ejoMBqNhBAPD4/12Zvd3NzhX8Nr1CIJJ5UpASGEuj1CKQWEEDIHvEYlZuUsp4B/DTWZ7u3Lqz64r+HmdfgbLOU2Pd8Mdpgxl/P0gr9Hp9P5DPS0sLB4f/SY0WM+JIQ0NTaqVBXFxWerKiuHDfNPSUt/3cmJEFJfXx8V+clPly7ZKBTpGauHDB16u7w8a/WqP/4oGx4QmLoyw9nZua2t7VhRYcryJIPB4Dd4SMSMmc4uLnV1dW1tbc7OLjY2Nr/99susiOkdHR0DvLyiohf36fvK96Xnvvryi7q6uuCQ0DVZ62x79QKAmKjIosKj7u4ei6JjXuvXr7ZGu27t6lu3bhmNxoWLokNHjRIKhRe//35t1mpra+u58xd8umQpADQ2NiYlxp0+eZKTyRISl4eEht7XaNJXplz+/Xcvr4Fr1q738PSEF9RqtX/d+ksqldr1tmtqaiop+S57w3qBQDBm7LjM1Vnwgq++/Dx3d45er1+wcNGkyVOsrK0qKioM7QZnFxeJRAIAfj7eWm3N0GH+M2bOksnlmurq3N05t279ZTQa01dlfjh2PMuyDQ0NqSuSTxw/JhKJPp42/f33Rzc16Xdu/+yXX37u6OjYum1H6FtvCYXCutpa30FehJCw8EkTwydZW1tXqlWbN2+6U1kZEhq6ZGm8i6vrjevXMzNWXrly2c9vSNa69X369tVqa/4oK/PxfUOpVBqNxuv/+WNy+MT29nYXV9fFsUsdHB0u/fDDju3btFrt4CFDN2/ZZmdvDwDJSQkF+Xlu7u4RETM9BwwwGAytrW2enp6sVAr/Gl6jFkk4qUwJCCHU7RFKKSCEkDngNSoxK2c5Bfx7KG1S3X5w6rj2h++f3K1qq9fBf8H0sLTq1Zsb4NUr9J2egSMtbRTw9+h0Op+BngAgFotFYjEAmEymttbW5ubmnra2KzMyR44caW0tAoAjhw+tX5v18OGD7Ts/f+/90YQQSulvv/06K2Iax3FLlsVP+GhiefmtjevWFRefCQgcsSI1zcXFFZ5Rq9Wuzsw4cvhQf2fnbZ/tdHVzA4Dm5uajRw+nLk/u06fvoujFYeHhABATFVlUeHR4QMDKjEwnp/7t7e1ff/H5zp3b9Xp9/r4Dw4cHMAzz6BE/feoUtUrlHxCQl78PAM6eOb12zerKSvWarHUTJoZZWloCQEXF7XFjRguthFFRMTNmzYb/FaX0wf37WzZnHz500Mvb+2jhcXjBpuyN+/bm2yhs4hOSgkNC4QV+Pt5abU1YWHhcYrJSqQSA+/c1UZELy65dDRgxInl5iouL64njx7Zszq64fXvJ0mVTp8+Qy+UA8ODB/cgF88uuXRv11jtp6Rn29vZ1tbW+g7wYhlmyNG7OvPlCobC5ufmzz7Z+u3+flZX1xs1bhg4ddvXqlZTlydXV9yZM+CglLR2ep9c3ZmakH9i/r0+fPl/u2u3h4QkAra2tp0+dXPrpYlvbXgsiF02PmAEAyUkJBfl5Q4f5r0hJ9fAcAADt7e0CgYBhGPjX8Bq1SMJJZUpACKFuj1BKASGEzAGvUYlZOcsp4F9FaVNFecOfN2oulOorbjVV3KZGIzzPUmbDurhxXgMVQ/w5jwHCnrbwt+l0Op+BnoSQ3v+fnR10qq2tffjgAQD07t07JW1l4IiR1tbWq9JX7s3PlUjY/H375XIb6HT3zp1V6Wnl5eXRMYsXRcdcvHBh2ZLY5ubmiBkzl8UnwPPu3Kn6eFJ4XV3tO+++t3nrZ/DUtatXliclVFVVfTx1WvKKVACIiYosKjwaEBiYlr7Kyak/AJSWnFv66WKdTpdXsD8wcAQhxGAwRC6YV3z2rIur69nvSgHgs61bPt+53dLScscXX/Xr5wCd6urqViQlXL/+n2nTZ6SkrYQXUErb2tpqamrUqorm5mZCyM8/Xdqbn+fs7FJcch5ecPDgN1/s3KGqqPB9w2/Kx1PfHBmkUCgIIfCUn4+3VlsTFhYel5isVCqh09Ytm/Jy9zAMs237ziFDhm5YtzZ3Tw7LSteu3xA44k14anVmxoF9BdbW1nv3f9O/v3Ndba3vIC+GYeLiE2fOniMUCgHg5IkTa7MyHz54+NWunJFBwRW3b2/K3nDq5AmFQjExfFL4pMn9+jkQQqCTVlsz/sMxjx7xI0cGf7krB54qv/XXsqWf3i4vHzdhwpqs9QCQnJRQkJ/nPzwgJTXN1c0dXgpeoxZJOKlMCQgh1O0RSikghJA54DUqMStnOQX8yyilhsf1TeqK2p8u6n77pfHPG4bGRuhELCys7OzlPn7KYQGyQT4i+74WIjH8Ezqdzmegp0AgmPfJgk8WRDIMAwCU0rKyaynLk+7euTN0mP+q1VmOjo4xUZFFhUcBgBACL4hcFL00Lv7M6VML5s+1s7Of/8mCiJmz4HkVt2+PChkp5bjpETOWLouHp26Xl6/NWl1y7rvxEz7K3rwVAGKiIosKjwYEBqalr3Jy6g8AV65c+WTuLK1Wm5ObHxQUTAgxGAxLYmOOFRU6ODqev/AjAKxMTdmd8zUAEELgBZMmT1mzdj08z2Qy3bh+feuWTRe+P28ymQghAGDq5OzsUlxyHl7Q3NyUvXHDkcOHGh4/NplM/fr1C588Zey4Cb179yaEAICfj7dWWxMWFh6XmKxUKqHT4UMHN2dvrK6+l5tf8ObIoIS4ZQf2Fwzy8V2Rmubj4wtP5e7ZvWP7tkc8f/J0sYenZ11tre8gL4Zh4uITZ86eIxQKAaC0tCQjLbWqqnLX7tyQ0FEGg+Hnny6tWJ6kqa42Go3W1tYhoaGRUTGurm6EkIcPHw4b7CsWiydN+XhFSho8de/u3dWZGWdOn3rr7Xe+/DoHAJKTEgry8/yHB6Skprm6ucNLwWvUIgknlSkBIYS6PUIpBYQQMge8RiVm5SyngJeCdnTo1RVNt289PHW8/o8rbTU1jKVQ1M+hZ8CbtiOCxE4uVr16EULgH9LpdD4DPQUCwaLomOiYWIZh4KnsDevycve0trYeOHjY23vQ4uhFRYVHra2t3/AbbGlpCc8gBN7/YPTYcRPOnD618JN5vXvbzf9kQcTMWfA8VUXF26OCxRLJ1GnT4+IT4anb5eVZazLPl5aM/2jixuzNABATFVlUeDQgMDAtfZWTU38AKCsrmzsrQqvV5uTmBwUFE0IMBsOS2JhjRYUOjo7nL/wIAOkrU3fv+trKymqQj49IJIZnEAIjRoycFjEDnnfz5o3sjetLz52ztbUdOnTYq/366fX633/99ebNG87OLsUl5+F/0tHR8dOlS/sK8n/95efGxkYACAkdFb041tNzAAD4+XhrtTVhYeFxiclKpRI6FRUVZq9fV119b3fe3jffHJmUELd/X4HXQO+UtJW+vm/AU3t25+zYvq1Wqz1x6oy7h2ddba3vIC+GYeLiE2fOniMUCgGgtLQkIy21qqpy1+7ckNBRAEApra6u/mb/vsKjR2pqHnV0dNjY2KxanfXue+/zPD98qJ9QKAyfPCU1LR2eunvnzurMjOKzZ956+50vvtoFAMlJCQX5ef7DA1JS01zd3OGl4DVqkYSTypSAEELdHqGUAkIImQNeoxKzcpZTwMtCTSbD43rdtcuaQwca/7rZQ8z2DAq2e3eM+LV+AgkLXaLT6XwGegoEgkXRMdExsQzDwFNfffH55zu319XVHSk85uP7xprMVfl5eywtLU+dPWdvbw//kx8uXoxbGqvX66dOi4hPTCKEwDPu3bs7feqUhw8eBIeE7vj8S0IIdLr8+28J8csePng4bXpEQlIyAMRERRYVHg0IDExLX+Xk1B8AysrK5s6K0Gq1Obn5QUHBhBCDwbAkNuZYUaGDo+P5Cz8CwI7t23Zu304IHPj2sLuHB/wNZ8+cjo2JltvI581bEDFzJgDodLq9ebnZG9c7O7sUl5yH/9Xt8vLcPTknTxwHIHPmzlsUHQMAfj7eWm1NWFh4XGKyUqmETju2b92dk8MQZutnO4YMHbo5e+PunK979LBctyE7OCQEnkpdkXzk8CGOk+3J2+vUv39dba3vIC+GYeLiE2fOniMUCgGgtLQkIy21qqpy1+7ckNBR8Ay9Xn/q5Imtmzfx/EMXF9dTZ7+r1WrDJo6vvndv6DD/vL37CCHQ6ebNG5/GRFVXV0/4KCwjczUAJCclFOTn+Q8PSElNc3Vzh5eC16hFEk4qUwJCCHV7hFIKCCFkDniNSszKWU4BL5dBr6/94ftHpcVWtr3s3hsjdfMgFhbQVTqdzmegp0AgWLgoKip6McMwAGAymfR6feSC+Vcu/25nZ//5l1+5uXsUHj2yLmvNgwf3o2NiF0Yu6mFpSQihlJpMJkKIQCAAgFu3/lq/Nqvk3HfDAwISk5a7uLoxDAMAJpOJYZjH9fXr1q755sD+119/ff3GTV4DvRmGaWhoOPjtgdWrMhwcHGNiY8eOmwAAMVGRRYVHAwID09JXOTn1B4CysrK5syK0Wm1Obn5QUDAhxGAwLImNOVZU6ODoeP7CjwDwXfHZtWtWq1QVc+bOi1wUzUqlDMNQSk0mEwD06NEDXnDi+LHoRQtf69cvOmbxh2PHMwxTV1e3Ny93U/YGZ2eX4pLz8AKj0WgymZhOAPDHH2Wfbd1y7rviGTNnp6VnAICfj7dWWzN6zIfL4hPt7OwopfX1uvhlSy79+OMwf/+k5Smurm5nz5zK3rih/NathYuipkfMsLXtRSmtVKsWx0TdvHHjg9Fjklek2NnZ19XW+g7yYhgmLj5x5uw5QqEQAEpLSzLSUquqKnftzg0JHWUymYxGIyHEwsICABobGwv25q3LWiMSif4sV+n1jeuy1uTn5drZ2W/Z9pmP7xsMwzQ16Y8VFi5PTuzTp090TGz45CkAkJyUUJCf5z88ICU1zdXNHV4KXqMWSTipTAkIIdTtEUopIISQOeA1KjErZzkFvHRt2hrtpYtCG4VyWADp0QP+D3Q6nc9AT4ZhAgNHhE+ZQghpffLkzp07xWfOqFQVhGFmzJg5Z+582169dHV1S5fE/nDxgtFo/HDs+JFBQT1tbet1ujtVVfZ9+owdN54Q8uTJk8Ijh1dlrGxra/MaOPDDseNe6+fQ0txcV1vrOcBrgJfXtatX5s2Zrdc3Oju7TJ0WYWdv9+svP39z4EBTk37U22+nrcywte0FADFRkUWFRwMCA9PSVzk59QeAsrKyubMitFptTm5+UFAwIcRgMCyJjTlWVOjg6Hj+wo8A8Li+Pn1l6skTxw0Gwzvvvjfqrbft7O3r6+vvVlWJxOJp0yMIIfC8Hy5eWJ6UWFNT4z98+Jx58+3t+2hrHh05cmjf3r3Ozi7FJefhBcVnz9y9c6dv376vOTgYjabzJecOH/q2TqebM3fe4tglAODn463V1rzyyivhk6Y4u7gaOgwnjx+7ePGC0WiM/XTJxLBJcrlcr9dnZqQfPXK4Rw/B6DEfBgWHtrQ07y/Ye+3a1R6WltmbtgQFB1taCutqa30HeTEMExefOHP2HKFQCAClpSUZaalVVZW7dueGhI66e/dO8ZkzErHYxc2NlXJVleqvv/z88uXLjq+/fq70gtFovPXnn9OmTmloeOzo6Dh9xqxXX331yuXfC/bubWh4PDxwxNp163v3tgOA5KSEgvw8/+EBKalprm7u8FLwGrVIwkllSkAIoW6PUEoBIYTMAa9RiVk5yyngpTO1t7fp6iyEQku5Dfzf1NfXBwwbbDAYOjo6TCYTABBCBAKBpaWlSCwOCBzx6ZKlr7zyKnQqL7+1IjnpxvXrbW1tRmMHADAMY2EhGD/ho/RVmUKhEAAe8Xxe3p69eblPnjwxGAyUUoZhlEplTOynH0+d3tLScvxY0YZ1WQ0NDQaDgVJqYWEhFAp9fd+IXbLMx9cXOsVERRYVHg0IDExLX+Xk1B8AysrK5s6K0Gq1Obn5QUHBhBCDwbAkNuZYUaGDo+P5Cz9CJ7VavToz/deff3ny5InJZKSUMgxjYSEYGRS8bfsOKysreB7PPyzYm5+3Z8+TJy0Gg4FSSgixsLAQCASubm6Fx07CCzIzVh46dLDh8WOTyQQAFhYWAoHAf3jAgoWLBg8ZAgB+Pt5abY2VlRWltL29nVLKMAzHyUJCQxdFxfRzcIBOFbdvb8re8MPFCy0tLUajkRAiEAjEYvH8TxZO+XgqJ5MBQF1tre8gL4Zh4uITZ86eIxQKAaC0tCQjLbWqqnLX7tyQ0FG//vpLQtzSe3fvGo1GACCEWFhYyGSyT5fGTfl4KgC0tbWdOX06MyPt8ePHBoOBUmphYWFpaTlggNfSuPjBQ4ZCp+SkhIL8PP/hASmpaa5u7vBS8Bq1SMJJZUpACKFuj1BKASGEzAGvUYlZOcspwJy1tLRszt7I8w/r6ur0jY0A0KNHD6Wyp6PT68OG+fsNHmJtbQ3PaGhoOPjtgZJz3927e6+jo8NGoXBzdXv7nXdHBgcLhULo1Nzc/MvPPxUeOfznX3816fWcXObp4REWPnnoMH8AMBgM5eV/7dm9++rl31taWuz79A0dNWrc+Al2dvbwVEF+3tWrV5xdXMaNn2Br2wsA7t27l/P1l3q9fvbceW5u7oQQo9G4f9/ea1ev9uzZMyFpOTyl1+uLCo8Unz2jVqvb29rlNnIXZ5fQt95+5933hEIhvECn0134/vvTp05WValbW1uFQqGtba9XXnvN18cnLHwyvKC05Nzp06duXv9PXV0dANjZ2wcEjPhg9GhXN3fo5OfjrdXWDPLxte9jr63Rtre3i0SigICA8RMm9razg2c8fvz4xIljp04cr6ysFFoK3d09Jk+dOnjwYCsra+jU1KRPXbGcEPLe+x+MeHOkQCAAgD//vFmqVlt6AAAD70lEQVRUeLRWq509Z567h0f1vXsHv/3m559+vH//vsFgYFnW3WPA2HHjQke9BU8ZjUa1WrVr19eXf/u1Sa/v3dsuKDhkYnh4nz594anDB7/96adLTv37jx033s7OHl4KXqMWSTipTAkIIdTtEUopIISQOeA1KjErZzkFIPQMPx9vrbYmLCw8LjHZysqqra2N4ziBQADoBbxGLZJwUpkSEEKo2yOUUkAIIXPAa1RiVs5yCkDoGX4+3lptTVhYeFxislKpBPTf8Rq1SMJJZUpACKFuj1BKASGEzAGvUYlZOcspAKFn+Pl4a7U1YWHhcYnJSqUS0H/Ha9QiCSeVKQEhhLo9QikFhBAyB7xGJWblLKcAhJ6Rsjy5sbFhyJChH4z5kGVZQP8dr1GLJJxUpgSEEOr2CKUUEELIHPAalZiVs5wCEEJdwmvUIgknlSkBIYS6PUIpBYQQMge8RiVm5SynAIRQl/AatUjCSWVKQAihbo9QSgEhhMwBr1GJWTnLKQAh1CW8Ri2ScFKZEhBCqNsjlFJACCFzwGtUYlbOcgpACHUJr1GLJJxUpgSEEOr2CKUUEELIHPAalZiVs5wCEEJdwmvUIgknlSkBIYS6PUIpBYQQMge8RiVm5SynAIRQl/AatUjCSWVKQAihbo9QSgEhhMwBr1GJWTnLKQAh1CW8Ri2ScFKZEhBCqNsjlFJACCFzwGtUYlbOcgpACHUJr1GLJJxUpgSEEOr2CKUUEELIHPAalZiVs5wCEEJdwmvUIgknlSkBIYS6PUIpBYQQMge8RiVm5SynAIRQl/AatUjCSWVKQAihbo9QSgEhhMwBr1GJWTnLKQAh1CW8Ri2ScFKZEhBCqNsjlFJACCFzwGtUYlbOcgpACHUJr1GLJJxUpgSEEOr2CKUUEELIHPAalZiVs5wCEEJdwmvUIgknlSkBIYS6PUIpBYQQMge8RiVm5SynAIRQl/AatUjCSWVKQAihbo9QSgEhhMwBr1GJWTnLKQAh1CW8Ri2ScFKZEhBCqNsjlFJACCFzwGtUYlbOcgpACHUJr1GLJJxUpgSEEOr2CKUUEELIHPAalZiVs5wCEEJdwmvUIgknlSkBIYS6PUIpBYQQMge8RiVm5SynAIRQl/AatUjCSWVKQAihbo9QSgEhhMzBo/uVVtYSzsYWEEL/HKUmXlPJcjYSqQ0ghFC3RyilgBBC5qBBV9PS0ihh5YIeloAQ+kcobWnRt7e2KHu92sNSCAgh1O0RSikghJA5oNT0uO5R65MmQAj9cwxjIZUprcVSQAghc0AopYAQQubDZDJRkxEQQv8MsRBYABBACCEzQSilgBBCCCGEEELdyf8Da5zp7vrEcagAAAAASUVORK5CYII=)