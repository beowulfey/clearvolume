package clearvolume.network.serialization;

import static java.lang.Math.toIntExact;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.SocketChannel;
import java.util.LinkedHashMap;
import java.util.Map;

import clearvolume.network.serialization.keyvalue.KeyValueMaps;
import clearvolume.volume.Volume;

public class ClearVolumeSerialization
{
	// first 4 digits of the CRC32 of 'ClearVolume' string :-)
	public static final int cStandardTCPPort = 9140;
	private static final int cLongSizeInBytes = Long.BYTES;

	public static final ByteBuffer serialize(	Volume<?> pVolume,
																						ByteBuffer pByteBuffer)
	{
		StringBuilder lStringBuilder = new StringBuilder();
		writeVolumeHeader(pVolume, lStringBuilder);

		final int lHeaderLength = lStringBuilder.length();

		final long lDataLength = pVolume.getDataSizeInBytes();
		int lNeededBufferLength = toIntExact(3 * cLongSizeInBytes
																					+ lHeaderLength
																					+ lDataLength);
		if (pByteBuffer == null || pByteBuffer.capacity() != lNeededBufferLength)
		{
			pByteBuffer = ByteBuffer.allocateDirect(lNeededBufferLength);
			pByteBuffer.order(ByteOrder.nativeOrder());
		}
		pByteBuffer.clear();

		pByteBuffer.putLong(lNeededBufferLength);
		pByteBuffer.putLong(lHeaderLength);
		pByteBuffer.put(lStringBuilder.toString().getBytes());
		pByteBuffer.putLong(lDataLength);
		pVolume.writeToByteBuffer(pByteBuffer);

		return pByteBuffer;
	};

	private static void writeVolumeHeader(Volume<?> pVolume,
																				StringBuilder pStringBuilder)
	{
		LinkedHashMap<String, String> lHeaderMap = new LinkedHashMap<String, String>();
		lHeaderMap.put("index", "" + pVolume.getTimeIndex());
		lHeaderMap.put("time", "" + pVolume.getTimeInSeconds());
		lHeaderMap.put("channel", "" + pVolume.getChannelID());
		lHeaderMap.put("channelname", pVolume.getChannelName());
		lHeaderMap.put("color", serializeFloatArray(pVolume.getColor()));
		lHeaderMap.put(	"viewmatrix",
										serializeFloatArray(pVolume.getViewMatrix()));
		lHeaderMap.put("dim", "" + pVolume.getDimension());
		lHeaderMap.put("type", "" + pVolume.getTypeName());
		lHeaderMap.put("bytespervoxel", "" + pVolume.getBytesPerVoxel());
		lHeaderMap.put("elementsize", "" + pVolume.getElementSize());
		lHeaderMap.put("width", "" + pVolume.getWidthInVoxels());
		lHeaderMap.put("height", "" + pVolume.getHeightInVoxels());
		lHeaderMap.put("depth", "" + pVolume.getDepthInVoxels());
		lHeaderMap.put(	"voxelwidth",
										"" + pVolume.getVoxelWidthInRealUnits());
		lHeaderMap.put(	"voxelheight",
										"" + pVolume.getVoxelHeightInRealUnits());
		lHeaderMap.put(	"voxeldepth",
										"" + pVolume.getVoxelDepthInRealUnits());
		lHeaderMap.put("realunit", pVolume.getRealUnitName());

		KeyValueMaps.writeStringFromMap(lHeaderMap, pStringBuilder);
	}

	static void readVolumeHeader(	ByteBuffer pByteBuffer,
																int pHeaderLength,
																Volume<?> pVolume)
	{

		Map<String, String> lHeaderMap = KeyValueMaps.readMapFromBuffer(pByteBuffer,
																																		pHeaderLength,
																																		null);
		final long lIndex = Long.parseLong(lHeaderMap.get("index"));
		final double lTime = Double.parseDouble(lHeaderMap.get("time"));
		final int lVolumeChannelID = Integer.parseInt(lHeaderMap.get("channel"));
		final String lVolumeChannelName = lHeaderMap.get("channelname");
		final float[] lColor = deserializeFloatArray(lHeaderMap.get("color"));
		final float[] lViewMatrix = deserializeFloatArray(lHeaderMap.get("viewmatrix"));
		final int lDim = Integer.parseInt(lHeaderMap.get("dim"));
		final String lType = lHeaderMap.get("type");
		final long lElementSize = Long.parseLong(lHeaderMap.get("elementsize"));
		final long lWidth = Long.parseLong(lHeaderMap.get("width"));
		final long lHeight = Long.parseLong(lHeaderMap.get("height"));
		final long lDepth = Long.parseLong(lHeaderMap.get("depth"));

		final String lRealUnitName = lHeaderMap.get("realunit");
		final double lVoxelWidth = Double.parseDouble(lHeaderMap.get("voxelwidth"));
		final double lVoxelHeight = Double.parseDouble(lHeaderMap.get("voxelheight"));
		final double lVoxelDepth = Double.parseDouble(lHeaderMap.get("voxeldepth"));

		pVolume.setTimeIndex(lIndex);
		pVolume.setTimeInSeconds(lTime);
		pVolume.setType(lType);
		pVolume.setChannelID(lVolumeChannelID);
		pVolume.setChannelName(lVolumeChannelName);
		pVolume.setColor(lColor);
		pVolume.setViewMatrix(lViewMatrix);
		pVolume.setDimension(lDim);
		pVolume.setDimensionsInVoxels(lElementSize,
																	lWidth,
																	lHeight,
																	lDepth);

		pVolume.setVoxelSizeInRealUnits(lRealUnitName,
																		lVoxelWidth,
																		lVoxelHeight,
																		lVoxelDepth);

	};

	private static String serializeFloatArray(float[] pFloatArray)
	{
		if (pFloatArray == null)
			return "";
		StringBuilder lStringBuilder = new StringBuilder();
		for (int i = 0; i < pFloatArray.length; i++)
		{
			final float lValue = pFloatArray[i];
			lStringBuilder.append(lValue);
			if (i != pFloatArray.length - 1)
				lStringBuilder.append(" ");
		}
		return lStringBuilder.toString();
	}

	private static float[] deserializeFloatArray(String pString)
	{
		if (pString == null || pString.isEmpty())
			return null;
		float[] lFloatArray;
		try
		{
			pString = pString.trim();
			String[] lSplittedString = pString.split(" ", -1);
			lFloatArray = new float[lSplittedString.length];
			for (int i = 0; i < lFloatArray.length; i++)
				lFloatArray[i] = Float.parseFloat(lSplittedString[i]);
			return lFloatArray;
		}
		catch (NumberFormatException e)
		{
			e.printStackTrace();
			return null;
		}
	}

	private static ThreadLocal<ByteBuffer> sScratchBufferThreadLocal = new ThreadLocal<ByteBuffer>();

	public static final <T> Volume<T> deserialize(SocketChannel pSocketChannel,
																								Volume<T> pVolume) throws IOException
	{
		if (pVolume == null)
		{
			pVolume = new Volume<T>();
		}

		ByteBuffer pScratchBuffer = sScratchBufferThreadLocal.get();
		if (pScratchBuffer == null || pScratchBuffer.capacity() == 0)
		{
			pScratchBuffer = ByteBuffer.allocateDirect(cLongSizeInBytes);
			pScratchBuffer.order(ByteOrder.nativeOrder());
		}

		readPartLength(pSocketChannel, pScratchBuffer);

		final int lHeaderLength = readPartLength(	pSocketChannel,
																							pScratchBuffer);

		pScratchBuffer = ensureScratchBufferLengthIsEnough(	pScratchBuffer,
																												lHeaderLength);

		readIntoScratchBuffer(pSocketChannel,
													pScratchBuffer,
													lHeaderLength);
		readVolumeHeader(pScratchBuffer, lHeaderLength, pVolume);

		final int lDataLength = readPartLength(	pSocketChannel,
																						pScratchBuffer);

		if (pScratchBuffer.capacity() < lDataLength)
		{
			pScratchBuffer = ByteBuffer.allocateDirect(lDataLength);
			pScratchBuffer.order(ByteOrder.nativeOrder());
		}

		readIntoScratchBuffer(pSocketChannel, pScratchBuffer, lDataLength);
		readVolumeData(pScratchBuffer, lDataLength, pVolume);

		sScratchBufferThreadLocal.set(pScratchBuffer);

		return pVolume;
	}

	private static void readIntoScratchBuffer(SocketChannel pSocketChannel,
																						ByteBuffer pScratchBuffer,
																						final int lHeaderLength) throws IOException
	{
		pScratchBuffer.clear();
		pScratchBuffer.limit(lHeaderLength);
		while (pScratchBuffer.hasRemaining())
		{
			pSocketChannel.read(pScratchBuffer);
			sleep();
		}
		pScratchBuffer.rewind();
	}

	private static ByteBuffer ensureScratchBufferLengthIsEnough(ByteBuffer pScratchBuffer,
																															final int lHeaderLength)
	{
		if (pScratchBuffer == null || pScratchBuffer.capacity() < lHeaderLength)
		{
			pScratchBuffer = ByteBuffer.allocateDirect(lHeaderLength);
			pScratchBuffer.order(ByteOrder.nativeOrder());
		}
		return pScratchBuffer;
	}

	private static int readPartLength(SocketChannel pSocketChannel,
																		ByteBuffer pScratchBuffer) throws IOException
	{
		pScratchBuffer.clear();
		pScratchBuffer.limit(Long.BYTES);
		while (pScratchBuffer.hasRemaining())
		{
			pSocketChannel.read(pScratchBuffer);
			sleep();
		}
		pScratchBuffer.rewind();
		final int lHeaderLength = toIntExact(pScratchBuffer.getLong());
		return lHeaderLength;
	};

	public static final <T> Volume<T> deserialize(ByteBuffer pByteBuffer,
																								Volume<T> pVolume)
	{
		pByteBuffer.rewind();
		final int lWholeLength = toIntExact(pByteBuffer.getLong());
		final int lHeaderLength = toIntExact(pByteBuffer.getLong());
		readVolumeHeader(pByteBuffer, lHeaderLength, pVolume);
		final long lDataLength = pByteBuffer.getLong();
		readVolumeData(pByteBuffer, lDataLength, pVolume);
		return pVolume;
	}

	static void readVolumeData(	ByteBuffer pByteBuffer,
															long pDataLength,
															Volume<?> pVolume)
	{

		if (pVolume.getDataBuffer() == null || pVolume.getDataBuffer()
																									.capacity() != pDataLength)
		{
			ByteBuffer lByteBuffer = ByteBuffer.allocateDirect(toIntExact(pDataLength));
			lByteBuffer.order(ByteOrder.nativeOrder());
			lByteBuffer.clear();
			pVolume.setDataBuffer(lByteBuffer);
		}

		pVolume.readFromByteBuffer(pByteBuffer);
	}

	private static void sleep()
	{
		try
		{
			Thread.sleep(1);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
	}

}
