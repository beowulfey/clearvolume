package clearvolume.demo;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;

import org.junit.Test;

import clearvolume.renderer.ClearVolumeRendererInterface;
import clearvolume.renderer.RenderAlgorithm;
import clearvolume.renderer.factory.ClearVolumeRendererFactory;
import clearvolume.transferf.TransferFunctions;
import coremem.ContiguousMemoryInterface;
import coremem.buffers.ContiguousBuffer;
import coremem.offheap.OffHeapMemory;
import coremem.types.NativeTypeEnum;
import coremem.util.Size;

import static com.oculusvr.capi.OvrLibrary.ovrDistortionCaps.*;
import static com.oculusvr.capi.OvrLibrary.ovrHmdType.*;
import static com.oculusvr.capi.OvrLibrary.ovrRenderAPIType.*;
import static com.oculusvr.capi.OvrLibrary.ovrTrackingCaps.*;

import com.oculusvr.capi.EyeRenderDesc;
import com.oculusvr.capi.FovPort;
import com.oculusvr.capi.GLTexture;
import com.oculusvr.capi.Hmd;
import com.oculusvr.capi.OvrLibrary;
import com.oculusvr.capi.OvrLibrary.ovrHmdCaps;
import com.oculusvr.capi.OvrVector2i;
import com.oculusvr.capi.OvrVector3f;
import com.oculusvr.capi.Posef;
import com.oculusvr.capi.RenderAPIConfig;
import com.oculusvr.capi.TextureHeader;
import com.sun.jna.Pointer;


public class ClearVolumeVRDemos
{

  public static void main(final String[] argv) throws ClassNotFoundException
  {
    if (argv.length == 0)
    {
      final Class<?> c = Class.forName("clearvolume.demo.ClearVolumeVRDemos");

      System.out.println("Give one of the following method names as parameter:");

      for (final Member m : c.getMethods())
      {
        final String name = ((Method) m).getName();

        if (name.substring(0, 4).equals("demo"))
        {
          System.out.println("Demo: " + ((Method) m).getName());
        }
      }
    }
    else
    {
      final ClearVolumeBasicDemos cvdemo = new ClearVolumeBasicDemos();
      Method m;

      try
      {
        m = cvdemo.getClass().getMethod(argv[0]);
      }
      catch (final Exception e)
      {
        System.out.println("Could not launch " + argv[0]
                + " because ...");
        e.printStackTrace();

        return;
      }

      try
      {
        System.out.println("Running " + argv[0] + "()...");
        m.invoke(cvdemo);
      }
      catch (final Exception e)
      {
        e.printStackTrace();
      }
    }

  }

  public static Hmd openFirstHmd() {
    Hmd hmd = Hmd.create(0);
    if(null == hmd) {
      return null;//hmd = Hmd.createDebug(OvrLibrary.ovrHmdType.ovrHmd_DK2);
    }
    return hmd;
  }

  @Test
  public void demoVRWith8BitGeneratedDataset() throws InterruptedException,
          IOException
  {

    final ClearVolumeRendererInterface lClearVolumeRenderer = ClearVolumeRendererFactory.newBestRenderer(	"ClearVolumeTest",
            1024,
            1024,
            NativeTypeEnum.UnsignedByte,
            1024,
            1024,
            1,
            false);
    lClearVolumeRenderer.setTransferFunction(TransferFunctions.getDefault());
    lClearVolumeRenderer.setVisible(true);

    final Hmd hmd;
    Hmd.initialize();

    hmd = openFirstHmd();
    if (0 == hmd.configureTracking(
            ovrTrackingCap_Orientation |
                    ovrTrackingCap_Position, 0)) {
      throw new IllegalStateException(
              "Unable to start the sensor");
    }

    System.err.println("Using Rift HMD: " + hmd.Type + ", sn=" + hmd.SerialNumber.toString() + ", res " + hmd.Resolution.w + "x" + hmd.Resolution.h);

    final int lResolutionX = 512;
    final int lResolutionY = lResolutionX;
    final int lResolutionZ = lResolutionX;

    final byte[] lVolumeDataArray = new byte[lResolutionX * lResolutionY
            * lResolutionZ];

    for (int z = 0; z < lResolutionZ; z++)
      for (int y = 0; y < lResolutionY; y++)
        for (int x = 0; x < lResolutionX; x++)
        {
          final int lIndex = x + lResolutionX
                  * y
                  + lResolutionX
                  * lResolutionY
                  * z;
          int lCharValue = (((byte) x ^ (byte) y ^ (byte) z));
          if (lCharValue < 12)
            lCharValue = 0;
          lVolumeDataArray[lIndex] = (byte) lCharValue;
        }

    lClearVolumeRenderer.setVolumeDataBuffer(	0,
            ByteBuffer.wrap(lVolumeDataArray),
            lResolutionX,
            lResolutionY,
            lResolutionZ);
    lClearVolumeRenderer.requestDisplay();

    while (lClearVolumeRenderer.isShowing())
    {
      Thread.sleep(500);
    }

    lClearVolumeRenderer.close();
    hmd.destroy();
    hmd.shutdown();
  }

  @Test
  public void demoWith8BitGeneratedDatasetTestReentrance() throws InterruptedException,
          IOException
  {

    for (int r = 0; r < 3; r++)
    {
      final ClearVolumeRendererInterface lClearVolumeRenderer = ClearVolumeRendererFactory.newBestRenderer(	"ClearVolumeTest",
              1024,
              1024,
              NativeTypeEnum.UnsignedByte,
              1024,
              1024,
              1,
              false);
      lClearVolumeRenderer.setTransferFunction(TransferFunctions.getDefault());
      lClearVolumeRenderer.setVisible(true);

      final int lResolutionX = 256;
      final int lResolutionY = lResolutionX;
      final int lResolutionZ = lResolutionX;

      final byte[] lVolumeDataArray = new byte[lResolutionX * lResolutionY
              * lResolutionZ];

      for (int z = 0; z < lResolutionZ; z++)
        for (int y = 0; y < lResolutionY; y++)
          for (int x = 0; x < lResolutionX; x++)
          {
            final int lIndex = x + lResolutionX
                    * y
                    + lResolutionX
                    * lResolutionY
                    * z;
            int lCharValue = (((byte) x ^ (byte) y ^ (byte) z));
            if (lCharValue < 12)
              lCharValue = 0;
            lVolumeDataArray[lIndex] = (byte) lCharValue;
          }

      lClearVolumeRenderer.setVolumeDataBuffer(	0,
              ByteBuffer.wrap(lVolumeDataArray),
              lResolutionX,
              lResolutionY,
              lResolutionZ);
      lClearVolumeRenderer.requestDisplay();

      while (lClearVolumeRenderer.isShowing())
      {
        Thread.sleep(500);
      }

      lClearVolumeRenderer.close();
    }
  }


  @Test
  public void demoVRWith16BitGeneratedDatasetStreaming() throws InterruptedException,
          IOException
  {
    final ClearVolumeRendererInterface lClearVolumeRenderer = ClearVolumeRendererFactory.newOpenCLRenderer(	"ClearVolumeTest",
            512,
            512,
            NativeTypeEnum.UnsignedShort,
            512,
            512,
            1,
            false);
    lClearVolumeRenderer.setTransferFunction(TransferFunctions.getDefault());
    lClearVolumeRenderer.setVisible(true);

    final int lResolutionX = 512;
    final int lResolutionY = lResolutionX;
    final int lResolutionZ = lResolutionX;

    final byte[] lVolumeDataArray = new byte[lResolutionX * lResolutionY
            * lResolutionZ
            * 2];

    int i = 0;
    while (lClearVolumeRenderer.isShowing())
    {

      for (int z = 0; z < lResolutionZ; z++)
        for (int y = 0; y < lResolutionY; y++)
          for (int x = 0; x < lResolutionX; x++)
          {
            final int lIndex = 2 * (x + lResolutionX * y + lResolutionX * lResolutionY
                    * z);
            lVolumeDataArray[lIndex + 1] += (byte) (((byte) x ^ (byte) y
                    ^ (byte) z ^ (byte) i));
          }

      lClearVolumeRenderer.setVolumeDataBuffer(	0,
              ByteBuffer.wrap(lVolumeDataArray),
              lResolutionX,
              lResolutionY,
              lResolutionZ);
      lClearVolumeRenderer.requestDisplay();

      // Thread.sleep(100);
      i++;
    }

    lClearVolumeRenderer.close();

  }

  @Test
  public void demoWith16BitIsoSurface()	throws InterruptedException,
          IOException
  {

    final int lMaxTextureRes = 2048;

    final ClearVolumeRendererInterface lClearVolumeRenderer = ClearVolumeRendererFactory.newBestRenderer(	"ClearVolumeTest",/**/
            768,
            768,
            NativeTypeEnum.UnsignedShort,
            lMaxTextureRes,
            lMaxTextureRes,
            1,
            false);
    lClearVolumeRenderer.setRenderAlgorithm(RenderAlgorithm.IsoSurface);
    lClearVolumeRenderer.setTransferFunction(TransferFunctions.getDefault());
    lClearVolumeRenderer.setVisible(true);

    final int lResolutionX = 512;
    final int lResolutionY = lResolutionX;
    final int lResolutionZ = lResolutionX;

    final byte[] lVolumeDataArray = new byte[lResolutionX * lResolutionY
            * lResolutionZ
            * 2];

    for (int z = 0; z < lResolutionZ; z++)
      for (int y = 0; y < lResolutionY; y++)
        for (int x = 0; x < lResolutionX; x++)
        {
          final int lIndex = 2 * (x + lResolutionX * y + lResolutionX * lResolutionY
                  * z);

          final double dist = (x - .3f * lResolutionX) * (x - .3f * lResolutionX)
                  + (y - .5f * lResolutionY)
                  * (y - .5f * lResolutionY)
                  + (z - .5f * lResolutionZ)
                  * (z - .5f * lResolutionZ);

          final double dist2 = (x - .7f * lResolutionX) * (x - .7f * lResolutionX)
                  + (y - .5f * lResolutionY)
                  * (y - .5f * lResolutionY)
                  + (z - .5f * lResolutionZ)
                  * (z - .5f * lResolutionZ);

          // lVolumeDataArray[lIndex + 1] = (byte) (255 * (Math
          // .exp(-0.001f * dist) + 1. * Math.exp(-0.001f
          // * dist2)));

          // final short lValue = (short) (32000 * (1. / (1 + 0.001 *
          // dist) + 1. / (1 + 0.001 * dist2)));
          // final short lValue = (short) (65000 * (1. / (1 + 0.001 * dist)));
          final short lValue = (short) (((short) x ^ (short) y ^ (short) z));

          lVolumeDataArray[lIndex] = (byte) (lValue & 0xFF);
          lVolumeDataArray[lIndex + 1] = (byte) ((lValue >> 8) & 0xFF);

        }

    lClearVolumeRenderer.setVolumeDataBuffer(	0,
            ByteBuffer.wrap(lVolumeDataArray),
            lResolutionX,
            lResolutionY,
            lResolutionZ);
    lClearVolumeRenderer.requestDisplay();

    while (lClearVolumeRenderer.isShowing())
    {
      Thread.sleep(100);
    }

    lClearVolumeRenderer.close();

  }

  @Test
  public void demoWithFileDatasets()
  {

    try
    {
      startSample("./data/Bucky.raw",
              NativeTypeEnum.UnsignedByte,
              32,
              32,
              32);
    }
    catch (final Throwable e)
    {
      e.printStackTrace();
    }

  }

  private static void startSample(final String pRessourceName,
                                  final NativeTypeEnum pNativeTypeEnum,
                                  final int pSizeX,
                                  final int pSizeY,
                                  final int pSizeZ)	throws IOException,
          InterruptedException
  {
    final InputStream lResourceAsStream = ClearVolumeBasicDemos.class.getResourceAsStream(pRessourceName);
    startSample(lResourceAsStream,
            pNativeTypeEnum,
            pSizeX,
            pSizeY,
            pSizeZ);
  }

  private static void startSample(final InputStream pInputStream,
                                  final NativeTypeEnum pNativeTypeEnum,
                                  final int pSizeX,
                                  final int pSizeY,
                                  final int pSizeZ)	throws IOException,
          InterruptedException
  {

    final byte[] data = loadData(	pInputStream,
            pNativeTypeEnum,
            pSizeX,
            pSizeY,
            pSizeZ);

    final ClearVolumeRendererInterface lClearVolumeRenderer = ClearVolumeRendererFactory.newBestRenderer(	"ClearVolumeTest",
            512,
            512,
            pNativeTypeEnum,
            false);

    lClearVolumeRenderer.setTransferFunction(TransferFunctions.getDefault());
    lClearVolumeRenderer.setVisible(true);

    lClearVolumeRenderer.setVolumeDataBuffer(	0,
            ByteBuffer.wrap(data),
            pSizeX,
            pSizeY,
            pSizeZ);

    lClearVolumeRenderer.requestDisplay();

    while (lClearVolumeRenderer.isShowing())
    {
      Thread.sleep(100);
    }

  }

  private static byte[] loadData(	final InputStream pInputStream,
                                   final NativeTypeEnum pNativeTypeEnum,
                                   final int sizeX,
                                   final int sizeY,
                                   final int sizeZ) throws IOException
  {
    // Try to read the specified file
    byte data[] = null;
    final InputStream fis = pInputStream;
    try
    {
      final int size = Size.of(pNativeTypeEnum) * sizeX
              * sizeY
              * sizeZ;
      data = new byte[size];
      fis.read(data);
    }
    catch (final IOException e)
    {
      System.err.println("Could not load input file");
      e.printStackTrace();
      return null;
    }
    fis.close();
    return data;
  }

  @SuppressWarnings("unused")
  private static byte[] loadData(	final String pRessourceName,
                                   final NativeTypeEnum pNativeTypeEnum,
                                   final int sizeX,
                                   final int sizeY,
                                   final int sizeZ) throws IOException
  {
    final InputStream lResourceAsStream = ClearVolumeBasicDemos.class.getResourceAsStream(pRessourceName);

    return loadData(lResourceAsStream,
            pNativeTypeEnum,
            sizeX,
            sizeY,
            sizeZ);
  }
}
