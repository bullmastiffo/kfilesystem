# KFileSystem - virtual file system, residing in a single file

KFileSystem aka __kfs__ is a virtual file system, residing in single file written completely in Kotlin.
File system structure is inspired by ext2/4fs filesystems family.
Consider this project a hackathon project as it was completed in short time, and it's author's first project for JVM in Kotlin.

lib folder contains all the code for file system itself.
cli folder contains primitive cli app to play around with virtual file system,
format, resize, copy between your OS file system and the virtual one.

## Filesystem structure and layout

Single unit of allocating virtual disk space is block.
kfs supports 1Kb, 2Kb and 4Kb block sizes. Block size defines maximum file size and filesystem capacity.
Every entry in file system (file, folder) is represented by inode. Each inode stores attributes for given
file system item and set of associated block, containing actual content.

The whole space allocated for file system is divided in block groups. Each block group keep track of set of inodes and blocks it contains.
Each block group contains 8 * `block size in bytes` data blocks.

|Block size:|1Kb|2Kb|4Kb|
|:---|-----|-----|-----|
|Block group size|8Mb|32Mb|128Mb|
|Maximum file size|16.14Mb (16 523Kb)|128.52Mb(131 606Kb)|1026.04 MB(105 0668Kb)|

Maximum size capacity can be increased by using more indirect blocks and/or double indirect blocks. (See inode below)

Maximum number of files is Int.MaxValue (2^31 - 1), but depends on actual file system size.

### Layout

|Super group|Block group 0|Block group 1|...|Block group N|
|---|---|---|---|---|
|1024 bytes| M data blocks|M data blocks|...|M data blocks|

Super group (class `SuperGroup`) contains general information about file system, like total size, number of data blocks, inodes etc.

As mentioned earlier size of each block group (class `BlockGroup`) is  M = 8 * `block size in bytes` data blocks.

#### inode structure

|Field|Type|Description|
|---|---|---|
|id|Int|inode identifier|
|type|NodeType|Enumeration, one of: File, Folder, None.|
|dataSize|Long|Data size in linked data blocks|
|created|Date?|Date item created, when inode is used.|
|lastModified|Date?|Date item last modifies, when inode is used.|
|blockOffsets|Array of Long|Array of data offsets|

`blockOffsets` array size is 13. First 11 items contain offsets to allocated data blocks.

One before last item contains offset to __indirect block__, block that contains offsets to actual data blocks.

The last item contains offset to __double indirect block__, block that contains offsets to other indirect blocks.

## Usage

To use file system first you need to format it, creating proper structure.
Using one of overloads of function `formatViFileSystem`, first parameter is either
`java.nio.channels.SeekableByteChannel` or `java.nio.file.Path`.

Settings parameter defines object with file system total size and block size.

```Kotlin
formatViFileSystem(virtualFsPath, settings)
```

Now it's possible to create an instance of `ViFileSystem` implementing `FileSystem` interface.
Providing instance of `java.nio.channels.SeekableByteChannel`.

`ViFileSystem` implements `java.io.Closeable` and will close provided channel on call to `close`.

```Kotlin
initializeViFilesystem(channel)
```

### Formatting limitation

Limitation of current formatter implementation is that you can won't get the exact size,
but the maximum number of block groups, that fit within requested size.
As an improvement to current implementation it's possible to add the last block group,
that will have fewer blocks then regular block groups.

### Resizing

You can upsize existing kfs filesystem without loosing content.
Providing instance of `java.nio.channels.SeekableByteChannel` and new total size in bytes.

Limitation of current implementation is minimum expansion step is block group size (8 * `block size in bytes`).

```Kotlin
resizeViFilesystem(channel, newSize)
```

## Generating documentation

For generating documentation just use dokka task

```bash
gradle dokkaGfm
```
