package com.creatorskit.models;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ModelData;
import okhttp3.*;

import javax.inject.Inject;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class ModelFinder
{
    @Inject
    OkHttpClient httpClient;
    @Getter
    private String lastFound;
    @Getter
    private int lastAnim;

    public ModelStats[] findModelsForPlayer(boolean groundItem, boolean maleItem, int[] items)
    {
        //Convert equipmentId to itemId or kitId as appropriate
        int[] ids = new int[items.length];
        ArrayList<Integer> itemList = new ArrayList<>();
        ArrayList<Integer> kitList = new ArrayList<>();

        for (int i = 0; i < ids.length; i++)
        {
            int item = items[i];

            if (item >= 256 && item <= 512)
                kitList.add(item - 256);

            if (item > 256)
                itemList.add(item - 512);
        }

        Pattern recolFrom = Pattern.compile("recol\\ds=.+");
        Pattern recolTo = Pattern.compile("recol\\dd=.+");

        ArrayList<ModelStats> modelStatsArray = new ArrayList<>();

        //for modelIds
        int[] itemId = new int[itemList.size()];
        for (int i = 0; i < itemList.size(); i++)
        {
            itemId[i] = itemList.get(i);
        }

        CountDownLatch countDownLatch = new CountDownLatch(2);

        Request itemRequest = new Request.Builder()
                .url("https://gitlab.com/waliedyassen/cache-dumps/-/raw/master/dump.obj")
                .build();
        Call itemCall = httpClient.newCall(itemRequest);
        itemCall.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e)
            {
                log.debug("Failed to access URL: https://gitlab.com/waliedyassen/cache-dumps/-/raw/master/dump.obj");
                countDownLatch.countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException
            {
                if (!response.isSuccessful() || response.body() == null)
                    return;

                getPlayerItems(response, modelStatsArray, groundItem, maleItem, itemId, recolFrom, recolTo);
                countDownLatch.countDown();
                response.body().close();
            }
        });

        //for KitIds
        int[] kitId = new int[kitList.size()];
        for (int i = 0; i < kitList.size(); i++)
        {
            kitId[i] = kitList.get(i);
        }

        Request kitRequest = new Request.Builder()
                .url("https://gitlab.com/waliedyassen/cache-dumps/-/raw/master/dump.idk")
                .build();
        Call kitCall = httpClient.newCall(kitRequest);
        kitCall.enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e)
            {
                log.debug("Failed to access URL: https://gitlab.com/waliedyassen/cache-dumps/-/raw/master/dump.idk");
                countDownLatch.countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException
            {
                if (!response.isSuccessful() || response.body() == null)
                    return;

                getPlayerKit(response, modelStatsArray, kitId, recolFrom, recolTo);
                countDownLatch.countDown();
                response.body().close();
            }
        });

        try
        {
            countDownLatch.await();
        }
        catch (Exception e)
        {
            log.debug("CountDownLatch failed to wait at findModelsForPlayers");
        }


        return modelStatsArray.toArray(new ModelStats[0]);
    }

    public static void getPlayerItems(Response response, ArrayList<ModelStats> modelStatsArray, boolean groundItem, boolean maleItem, int[] itemId, Pattern recolFrom, Pattern recolTo)
    {
        InputStream inputStream = response.body().byteStream();
        Scanner scanner = new Scanner(inputStream);
        Pattern[] patterns = new Pattern[itemId.length];

        for (int i = 0; i < itemId.length; i++)
        {
            int item = itemId[i];
            Pattern itemPattern = Pattern.compile("\\[.+_" + item + "]");
            patterns[i] = itemPattern;
        }

        while (scanner.hasNextLine())
        {
            String string = scanner.nextLine();
            for (int i = 0; i < patterns.length; i++)
            {
                Pattern pattern = patterns[i];
                Matcher matcher = pattern.matcher(string);
                if (matcher.matches())
                {
                    int[] modelIds = new int[3];
                    ArrayList<Integer> recolourFrom = new ArrayList<>();
                    ArrayList<Integer> recolourTo = new ArrayList<>();

                    while (!string.isEmpty())
                    {
                        string = scanner.nextLine();
                        if (groundItem && string.startsWith("model="))
                        {
                            String[] split = string.split("_");
                            modelIds[0] = Integer.parseInt(split[split.length - 1]);
                        }
                        else if (maleItem && string.startsWith("manwear="))
                        {
                            String replaced = string.replace(",", "_");
                            String[] split = replaced.split("_");
                            modelIds[0] = Integer.parseInt(split[split.length - 2]);
                        }
                        else if (maleItem && string.startsWith("manwear2="))
                        {
                            String[] split = string.split("_");
                            modelIds[1] = Integer.parseInt(split[split.length - 1]);
                        }
                        else if (maleItem && string.startsWith("manwear3="))
                        {
                            String[] split = string.split("_");
                            modelIds[2] = Integer.parseInt(split[split.length - 1]);
                        }
                        else if (!maleItem && string.startsWith("womanwear="))
                        {
                            String replaced = string.replace(",", "_");
                            String[] split = replaced.split("_");
                            modelIds[0] = Integer.parseInt(split[split.length - 2]);
                        }
                        else if (!maleItem && string.startsWith("womanwear2="))
                        {
                            String[] split = string.split("_");
                            modelIds[1] = Integer.parseInt(split[split.length - 1]);
                        }
                        else if (!maleItem && string.startsWith("womanwear3="))
                        {
                            String[] split = string.split("_");
                            modelIds[2] = Integer.parseInt(split[split.length - 1]);
                        }
                        else if (string.startsWith("recol"))
                        {
                            matcher = recolFrom.matcher(string);

                            if (matcher.matches())
                            {
                                String[] split = string.split("=");
                                recolourFrom.add(Integer.parseInt(split[1]));
                                continue;
                            }

                            matcher = recolTo.matcher(string);

                            if (matcher.matches())
                            {
                                String[] split = string.split("=");
                                recolourTo.add(Integer.parseInt(split[1]));
                            }
                        }
                    }

                    int size = recolourFrom.size();
                    short[] recolourFromArray = new short[size];
                    short[] recolourToArray = new short[size];
                    for (int e = 0; e < size; e++)
                    {
                        int from = recolourFrom.get(e);
                        if (from > 32767)
                            from -= 65536;
                        recolourFromArray[e] = (short) from;

                        int to = recolourTo.get(e);
                        if (to > 32767)
                            to -= 65536;
                        recolourToArray[e] = (short) to;
                    }

                    for (int id : modelIds)
                    {
                        if (id > 0)
                        {
                            modelStatsArray.add(new ModelStats(
                                    id,
                                    BodyPart.NA,
                                    recolourFromArray,
                                    recolourToArray,
                                    128,
                                    128,
                                    128,
                                    new Lighting(64, 850, -30, -50, -30)));
                        }
                    }

                    if (i == patterns.length - 1)
                    {
                        break;
                    }
                }
            }
        }
    }

    public static void getPlayerKit(Response response, ArrayList<ModelStats> modelStatsArray, int[] kitId, Pattern recolFrom, Pattern recolTo)
    {
        InputStream inputStream = response.body().byteStream();
        Scanner scanner = new Scanner(inputStream);
        Pattern[] patterns = new Pattern[kitId.length];

        for (int i = 0; i < kitId.length; i++)
        {
            //Create a pattern to match the format [idk_XXX] per kitId
            int item = kitId[i];
            Pattern kitPattern = Pattern.compile("\\[.+_" + item + "]");
            patterns[i] = kitPattern;
        }

        while (scanner.hasNextLine())
        {
            String string = scanner.nextLine();
            for (int i = 0; i < patterns.length; i++)
            {
                Pattern pattern = patterns[i];
                Matcher matcher = pattern.matcher(string);
                if (matcher.matches())
                {
                    int[] modelIds = new int[2];
                    ArrayList<Integer> recolourFrom = new ArrayList<>();
                    ArrayList<Integer> recolourTo = new ArrayList<>();
                    BodyPart bodyPart = BodyPart.NA;

                    while (!string.isEmpty())
                    {
                        string = scanner.nextLine();
                        if (string.startsWith("model1="))
                        {
                            String[] split = string.split("_");
                            modelIds[0] = Integer.parseInt(split[split.length - 1]);
                        }
                        else if (string.startsWith("model2="))
                        {
                            String[] split = string.split("_");
                            modelIds[1] = Integer.parseInt(split[split.length - 1]);
                        }
                        else if (string.startsWith("bodypart"))
                        {
                            if (string.endsWith("hair"))
                                bodyPart = BodyPart.HAIR;
                            else if (string.endsWith("jaw"))
                                bodyPart = BodyPart.JAW;
                            else if (string.endsWith("torso"))
                                bodyPart = BodyPart.TORSO;
                            else if (string.endsWith("arms"))
                                bodyPart = BodyPart.ARMS;
                            else if (string.endsWith("hands"))
                                bodyPart = BodyPart.HANDS;
                            else if (string.endsWith("legs"))
                                bodyPart = BodyPart.LEGS;
                            else if (string.endsWith("feet"))
                                bodyPart = BodyPart.FEET;
                        }
                        else if (string.startsWith("recol"))
                        {
                            //recolour if a recol1s and recol1d are present with the kitId
                            matcher = recolFrom.matcher(string);

                            if (matcher.matches())
                            {
                                String[] split = string.split("=");
                                recolourFrom.add(Integer.parseInt(split[1]));
                                continue;
                            }

                            matcher = recolTo.matcher(string);

                            if (matcher.matches())
                            {
                                String[] split = string.split("=");
                                recolourTo.add(Integer.parseInt(split[1]));
                            }
                        }
                    }

                    int size = recolourFrom.size();
                    short[] recolourFromArray = new short[size];
                    short[] recolourToArray = new short[size];
                    for (int e = 0; e < size; e++)
                    {
                        int from = recolourFrom.get(e);
                        if (from > 32767)
                            from -= 65536;
                        recolourFromArray[e] = (short) from;

                        int to = recolourTo.get(e);
                        if (to > 32767)
                            to -= 65536;
                        recolourToArray[e] = (short) to;
                    }

                    for (int id : modelIds)
                    {
                        if (id > 0)
                        {
                            modelStatsArray.add(new ModelStats(
                                    id,
                                    bodyPart,
                                    recolourFromArray,
                                    recolourToArray,
                                    128,
                                    128,
                                    128,
                                    new Lighting(64, 850, -30, -50, -30)));
                        }
                    }

                    if (i == patterns.length - 1)
                    {
                        break;
                    }
                }
            }
        }
    }

    public ModelStats[] findSpotAnim(int spotAnimId)
    {
        Pattern recolFrom = Pattern.compile("recol\\ds=.+");
        Pattern recolTo = Pattern.compile("recol\\dd=.+");

        ArrayList<Integer> modelIds = new ArrayList<>();
        final int[] resize = new int[]{128, 128, 128};
        ArrayList<Short> recolourFrom = new ArrayList<>();
        ArrayList<Short> recolourTo = new ArrayList<>();
        Lighting lighting = new Lighting(ModelData.DEFAULT_AMBIENT, ModelData.DEFAULT_CONTRAST, ModelData.DEFAULT_X, ModelData.DEFAULT_Y, ModelData.DEFAULT_Z);

        CountDownLatch countDownLatch = new CountDownLatch(1);
        Request request = new Request.Builder()
                .url("https://gitlab.com/waliedyassen/cache-dumps/-/raw/master/dump.spotanim?ref_type=heads")
                .build();
        Call call = httpClient.newCall(request);
        call.enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException e)
            {
                log.debug("Failed to access URL: https://gitlab.com/waliedyassen/cache-dumps/-/raw/master/dump.spotanim?ref_type=heads");
                countDownLatch.countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException
            {
                if (!response.isSuccessful() || response.body() == null)
                    return;

                InputStream inputStream = response.body().byteStream();
                Scanner scanner = new Scanner(inputStream);
                Pattern npcPattern = Pattern.compile("\\[.+_" + spotAnimId + "]");

                while (scanner.hasNextLine())
                {
                    String string = scanner.nextLine();
                    Matcher match = npcPattern.matcher(string);
                    if (match.matches())
                    {
                        lastFound = "SpotAnim " + spotAnimId;
                        lastAnim = -1;
                        while (!string.isEmpty())
                        {
                            string = scanner.nextLine();
                            if (string.startsWith("model"))
                            {
                                String[] split = string.split("_");
                                modelIds.add(Integer.parseInt(split[split.length - 1]));
                            }
                            else if (string.startsWith("anim"))
                            {
                                String[] split = string.split("_");
                                lastAnim = Integer.parseInt(split[split.length - 1]);
                            }
                            else if (string.startsWith("amb"))
                            {
                                String[] split = string.split("=");
                                int ambient = Integer.parseInt(split[split.length - 1]);
                                if (ambient >= 128)
                                    ambient -= 256;

                                lighting.setAmbient(64 + ambient);
                            }
                            else if (string.startsWith("con"))
                            {
                                String[] split = string.split("=");
                                int contrast = Integer.parseInt(split[split.length - 1]);
                                if (contrast >= 128)
                                    contrast -= 128;

                                lighting.setContrast(768 + contrast);
                            }
                            else if (string.startsWith("resizeh"))
                            {
                                String[] split = string.split("=");
                                int resizeH = Integer.parseInt(split[split.length - 1]);
                                resize[0] = resizeH;
                                resize[1] = resizeH;
                            }
                            else if (string.startsWith("resizev"))
                            {
                                String[] split = string.split("=");
                                resize[2] = Integer.parseInt(split[split.length - 1]);
                            }
                            else
                            {
                                match = recolFrom.matcher(string);

                                if (match.matches())
                                {
                                    String[] split = string.split("=");
                                    int i = Integer.parseInt(split[1]);
                                    if (i > 32767)
                                    {
                                        i -= 65536;
                                    }
                                    recolourFrom.add((short) i);
                                }

                                match = recolTo.matcher(string);

                                if (match.matches())
                                {
                                    String[] split = string.split("=");
                                    int i = Integer.parseInt(split[1]);
                                    if (i > 32767)
                                    {
                                        i -= 65536;
                                    }
                                    recolourTo.add((short) i);
                                }
                            }
                        }
                    }
                }
                countDownLatch.countDown();
                response.body().close();
            }
        });

        try
        {
            countDownLatch.await();
        }
        catch (Exception e)
        {
            log.debug("CountDownLatch failed to await at findModelsForNPCs");
        }

        short[] rf = new short[recolourFrom.size()];
        short[] rt = new short[recolourTo.size()];
        for (int i = 0; i < recolourFrom.size(); i++)
        {
            rf[i] = recolourFrom.get(i);
            rt[i] = recolourTo.get(i);
        }

        return new ModelStats[]{new ModelStats(
                modelIds.get(0),
                BodyPart.NA,
                rf,
                rt,
                resize[0],
                resize[1],
                resize[2],
                lighting)};
    }

    public ModelStats[] findModelsForNPC(int npcId)
    {
        Pattern recolFrom = Pattern.compile("recol\\ds=.+");
        Pattern recolTo = Pattern.compile("recol\\dd=.+");

        ArrayList<Integer> modelIds = new ArrayList<>();
        final int[] resize = new int[]{128, 128, 128};
        ArrayList<Short> recolourFrom = new ArrayList<>();
        ArrayList<Short> recolourTo = new ArrayList<>();

        CountDownLatch countDownLatch = new CountDownLatch(1);
        Request request = new Request.Builder()
                .url("https://gitlab.com/waliedyassen/cache-dumps/-/raw/master/dump.npc")
                .build();
        Call call = httpClient.newCall(request);
        call.enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException e)
            {
                log.debug("Failed to access URL: https://gitlab.com/waliedyassen/cache-dumps/-/raw/master/dump.npc");
                countDownLatch.countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException
            {
                if (!response.isSuccessful() || response.body() == null)
                    return;

                InputStream inputStream = response.body().byteStream();
                Scanner scanner = new Scanner(inputStream);
                Pattern npcPattern = Pattern.compile("\\[.+_" + npcId + "]");

                while (scanner.hasNextLine())
                {
                    String string = scanner.nextLine();
                    Matcher match = npcPattern.matcher(string);
                    if (match.matches())
                    {
                        lastFound = string;
                        while (!string.isEmpty())
                        {
                            string = scanner.nextLine();

                            if (string.startsWith("name="))
                            {
                                lastFound = string.replaceAll("name=", "");
                            }
                            else if (string.startsWith("model"))
                            {
                                String[] split = string.split("_");
                                modelIds.add(Integer.parseInt(split[split.length - 1]));
                            }
                            else if (string.startsWith("recol"))
                            {
                                match = recolFrom.matcher(string);

                                if (match.matches())
                                {
                                    String[] split = string.split("=");
                                    int i = Integer.parseInt(split[1]);
                                    if (i > 32767)
                                    {
                                        i -= 65536;
                                    }
                                    recolourFrom.add((short) i);
                                }

                                match = recolTo.matcher(string);

                                if (match.matches())
                                {
                                    String[] split = string.split("=");
                                    int i = Integer.parseInt(split[1]);
                                    if (i > 32767)
                                    {
                                        i -= 65536;
                                    }
                                    recolourTo.add((short) i);
                                }
                            }
                            else if (string.startsWith("resizex"))
                            {
                                String[] split = string.split("=");
                                resize[0] = Integer.parseInt(split[1]);
                            }
                            else if (string.startsWith("resizey"))
                            {
                                String[] split = string.split("=");
                                resize[2] = Integer.parseInt(split[1]);
                            }
                            else if (string.startsWith("resizez"))
                            {
                                String[] split = string.split("=");
                                resize[1] = Integer.parseInt(split[1]);
                            }
                        }
                    }
                }
                countDownLatch.countDown();
                response.body().close();
            }
        });

        try
        {
            countDownLatch.await();
        }
        catch (Exception e)
        {
            log.debug("CountDownLatch failed to await at findModelsForNPCs");
        }

        short[] rf = new short[recolourFrom.size()];
        short[] rt = new short[recolourTo.size()];
        for (int i = 0; i < recolourFrom.size(); i++)
        {
            rf[i] = recolourFrom.get(i);
            rt[i] = recolourTo.get(i);
        }

        ModelStats[] modelStats = new ModelStats[modelIds.size()];
        for (int i = 0; i < modelIds.size(); i++)
            modelStats[i] = new ModelStats(
                    modelIds.get(i),
                    BodyPart.NA,
                    rf,
                    rt,
                    resize[0],
                    resize[1],
                    resize[2],
                    new Lighting(64, 850, -30, -50, -30));

        return modelStats;
    }

    public ModelStats[] findModelsForObject(int objectId)
    {
        Pattern recolFrom = Pattern.compile("recol\\ds=.+");
        Pattern recolTo = Pattern.compile("recol\\dd=.+");

        ArrayList<Integer> modelIds = new ArrayList<>();
        final int[] resize = new int[]{128, 128, 128};
        ArrayList<Short> recolourFrom = new ArrayList<>();
        ArrayList<Short> recolourTo = new ArrayList<>();

        CountDownLatch countDownLatch = new CountDownLatch(1);
        Request request = new Request.Builder()
                .url("https://gitlab.com/waliedyassen/cache-dumps/-/raw/master/dump.loc")
                .build();
        Call call = httpClient.newCall(request);
        call.enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException e)
            {
                log.debug("Failed to access URL: https://gitlab.com/waliedyassen/cache-dumps/-/raw/master/dump.loc");
                countDownLatch.countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException
            {
                if (!response.isSuccessful() || response.body() == null)
                    return;

                InputStream inputStream = response.body().byteStream();
                Scanner scanner = new Scanner(inputStream);
                Pattern npcPattern = Pattern.compile("\\[.+_" + objectId + "]");

                while (scanner.hasNextLine())
                {
                    String string = scanner.nextLine();
                    Matcher match = npcPattern.matcher(string);
                    if (match.matches())
                    {
                        lastFound = string;
                        while (!string.isEmpty())
                        {
                            string = scanner.nextLine();
                            if (string.startsWith("name"))
                            {
                                lastFound = string.replaceAll("name=", "");
                            }
                            else if (string.startsWith("model"))
                            {
                                String[] split = string.split("_");
                                if (split[split.length - 1].contains(","))
                                {
                                    String split2 = split[split.length - 1].split(",")[0];
                                    modelIds.add(Integer.parseInt(split2));
                                }
                                else
                                {
                                    modelIds.add(Integer.parseInt(split[split.length - 1]));
                                }
                            }
                            else if (string.startsWith("recol"))
                            {
                                match = recolFrom.matcher(string);

                                if (match.matches())
                                {
                                    String[] split = string.split("=");
                                    int i = Integer.parseInt(split[1]);
                                    if (i > 32767)
                                    {
                                        i -= 65536;
                                    }
                                    recolourFrom.add((short) i);
                                }

                                match = recolTo.matcher(string);

                                if (match.matches())
                                {
                                    String[] split = string.split("=");
                                    int i = Integer.parseInt(split[1]);
                                    if (i > 32767)
                                    {
                                        i -= 65536;
                                    }
                                    recolourTo.add((short) i);
                                }
                            }
                            else if (string.startsWith("resizex"))
                            {
                                String[] split = string.split("=");
                                resize[0] = Integer.parseInt(split[1]);
                            }
                            else if (string.startsWith("resizey"))
                            {
                                String[] split = string.split("=");
                                resize[2] = Integer.parseInt(split[1]);
                            }
                            else if (string.startsWith("resizez"))
                            {
                                String[] split = string.split("=");
                                resize[1] = Integer.parseInt(split[1]);
                            }
                        }
                    }
                }
                countDownLatch.countDown();
                response.body().close();
            }
        });

        try
        {
            countDownLatch.await();
        }
        catch (Exception e)
        {
            log.debug("CountDownLatch failed to await at findModelsForObject");
        }

        short[] rf = new short[recolourFrom.size()];
        short[] rt = new short[recolourTo.size()];
        for (int i = 0; i < recolourFrom.size(); i++)
        {
            rf[i] = recolourFrom.get(i);
            rt[i] = recolourTo.get(i);
        }

        ModelStats[] modelStats = new ModelStats[modelIds.size()];
        for (int i = 0; i < modelIds.size(); i++)
            modelStats[i] = new ModelStats(
                    modelIds.get(i),
                    BodyPart.NA,
                    rf,
                    rt,
                    resize[0],
                    resize[1],
                    resize[2],
                    new Lighting(ModelData.DEFAULT_AMBIENT, ModelData.DEFAULT_CONTRAST, ModelData.DEFAULT_X, ModelData.DEFAULT_Y, ModelData.DEFAULT_Z));

        return modelStats;
    }

    public ModelStats[] findModelsForGroundItem(int itemId, CustomModelType modelType)
    {
        Pattern recolFrom = Pattern.compile("recol\\ds=.+");
        Pattern recolTo = Pattern.compile("recol\\dd=.+");

        ArrayList<Integer> modelIds = new ArrayList<>();
        ArrayList<Short> recolourFrom = new ArrayList<>();
        ArrayList<Short> recolourTo = new ArrayList<>();

        CountDownLatch countDownLatch = new CountDownLatch(1);
        Request request = new Request.Builder()
                .url("https://gitlab.com/waliedyassen/cache-dumps/-/raw/master/dump.obj")
                .build();
        Call call = httpClient.newCall(request);
        call.enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException e)
            {
                log.debug("Failed to access URL: https://gitlab.com/waliedyassen/cache-dumps/-/raw/master/dump.obj");
                countDownLatch.countDown();
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException
            {
                if (!response.isSuccessful() || response.body() == null)
                    return;

                InputStream inputStream = response.body().byteStream();
                Scanner scanner = new Scanner(inputStream);
                Pattern npcPattern = Pattern.compile("\\[.+_" + itemId + "]");

                while (scanner.hasNextLine())
                {
                    String string = scanner.nextLine();
                    Matcher match = npcPattern.matcher(string);
                    if (match.matches())
                    {
                        lastFound = string;
                        while (!string.isEmpty())
                        {
                            string = scanner.nextLine();
                            String searchName;
                            switch (modelType)
                            {
                                default:
                                case CACHE_GROUND_ITEM:
                                    searchName = "model";
                                    break;
                                case CACHE_MAN_WEAR:
                                    searchName = "manwear";
                                    break;
                                case CACHE_WOMAN_WEAR:
                                    searchName = "womanwear";
                            }

                            if (string.startsWith("name="))
                            {
                                lastFound = string.replaceAll("name=", "");
                            }
                            else if (string.startsWith(searchName))
                            {
                                String[] split = string.split("_");
                                if (split[split.length - 1].contains(","))
                                {
                                    String split2 = split[split.length - 1].split(",")[0];
                                    modelIds.add(Integer.parseInt(split2));
                                }
                                else
                                {
                                    modelIds.add(Integer.parseInt(split[split.length - 1]));
                                }
                            }
                            else
                            {
                                match = recolFrom.matcher(string);

                                if (match.matches())
                                {
                                    String[] split = string.split("=");
                                    int i = Integer.parseInt(split[1]);
                                    if (i > 32767)
                                    {
                                        i -= 65536;
                                    }
                                    recolourFrom.add((short) i);
                                }

                                match = recolTo.matcher(string);

                                if (match.matches())
                                {
                                    String[] split = string.split("=");
                                    int i = Integer.parseInt(split[1]);
                                    if (i > 32767)
                                    {
                                        i -= 65536;
                                    }
                                    recolourTo.add((short) i);
                                }
                            }
                        }
                    }
                }
                countDownLatch.countDown();
                response.body().close();
            }
        });

        try
        {
            countDownLatch.await();
        }
        catch (Exception e)
        {
            log.debug("CountDownLatch failed to await at findModelsForGroundItem");
        }

        if (modelIds.isEmpty())
            return null;

        short[] rf = new short[recolourFrom.size()];
        short[] rt = new short[recolourTo.size()];
        for (int i = 0; i < recolourFrom.size(); i++)
        {
            rf[i] = recolourFrom.get(i);
            rt[i] = recolourTo.get(i);
        }

        ModelStats[] modelStats = new ModelStats[modelIds.size()];
        for (int i = 0; i < modelIds.size(); i++)
            modelStats[i] = new ModelStats(
                    modelIds.get(i),
                    BodyPart.NA,
                    rf,
                    rt,
                    128,
                    128,
                    128,
                    new Lighting(ModelData.DEFAULT_AMBIENT, ModelData.DEFAULT_CONTRAST, ModelData.DEFAULT_X, ModelData.DEFAULT_Y, ModelData.DEFAULT_Z));

        return modelStats;
    }

    public static String shortArrayToString(short[] array)
    {
        StringBuilder stringBuilder = new StringBuilder();
        for (int i = 0; i < array.length; i++)
        {
            short s = array[i];
            stringBuilder.append(s);
            if (i < array.length - 1)
                stringBuilder.append(",");
        }

        return stringBuilder.toString();
    }
}
