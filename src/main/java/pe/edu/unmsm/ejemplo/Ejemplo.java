/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 */

package pe.edu.unmsm.ejemplo;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.concurrent.*;

/*
This class contains details such as the word frequency count for each term and term frequency for each term of the document.
  
    Tarea: Paralelizar algoritmo TF-IDF
   
    Integrantes:
    
    -Escalante Flores, Eduardo       18200147
    -Tovar Canturin, Daniel Ariel    20200096
    -Sifuentes Marcelo, Roberto      18200067

 */
 class DocumentProperties{


public

     HashMap<String,Double> getTermFreqMap()
     {
         return termFreqMap;
     }

     HashMap<String,Integer> getWordCountMap()
     {
         return DocWordCounts;
     }

     void setTermFreqMap(HashMap<String,Double> inMap)
     {
          termFreqMap = new HashMap<String, Double>(inMap);
     }


     void setWordCountMap(HashMap<String,Integer> inMap)
     {
         DocWordCounts =new HashMap<String, Integer>(inMap);
     }
private
     HashMap<String,Double> termFreqMap ;
     HashMap<String,Integer> DocWordCounts;
}


public class Ejemplo {

    SortedSet<String> wordList = new TreeSet(String.CASE_INSENSITIVE_ORDER);

    //Calcula la frecuencia inversa del documento.
   public HashMap<String,Double> calculateInverseDocFrequency(DocumentProperties [] docProperties)
    {

      HashMap<String,Double> InverseDocFreqMap = new HashMap<>();
        int size = docProperties.length;
        double wordCount ;
        for (String word : wordList) {
            wordCount = 0;
            for(int i=0;i<size;i++)
            {
                HashMap<String,Integer> tempMap = docProperties[i].getWordCountMap();
                if(tempMap.containsKey(word))
                {
                    wordCount++;
                    continue;
                }
            }
            double temp = size/ wordCount;
            double idf = 1 + Math.log(temp);
            InverseDocFreqMap.put(word,idf);
        }
        return InverseDocFreqMap;
    }

    //Calcula la frecuencia de término para todos los términos.
    public HashMap<String,Double> calculateTermFrequency(HashMap<String,Integer>inputMap) {

        HashMap<String ,Double> termFreqMap = new HashMap<>();
        double sum = 0.0;
        //Get the sum of all elements in hashmap
        for (float val : inputMap.values()) {
            sum += val;
        }

        //Obtén la suma de todos los elementos en el HashMap.
        Iterator it = inputMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            double tf = (Integer)pair.getValue()/ sum;
            termFreqMap.put((pair.getKey().toString()),tf);
        }
        return termFreqMap;
    }

    //Devuelve si la entrada contiene números o no.
    public  boolean isDigit(String input)
    {
        String regex = "(.)*(\\d)(.)*";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(input);

        boolean isMatched = matcher.matches();
        if (isMatched) {
            return true;
        }
        return false;
    }

    //Escribe el contenido del HashMap en un archivo CSV.
    public  void outputAsCSV(HashMap<String,Double>treeMap,String OutputPath) throws IOException {
        StringBuilder builder = new StringBuilder();
        for (Map.Entry<String, Double> keymap : treeMap.entrySet()) {
            builder.append(keymap.getKey());
            builder.append(",");
            builder.append(keymap.getValue());
            builder.append("\r\n");
        }
        String content = builder.toString().trim();
        BufferedWriter writer = new BufferedWriter(new FileWriter(OutputPath));
        writer.write(content);
        writer.close();
    }
    //Limpieza de la entrada eliminando . , : "
    public  String cleanseInput(String input)
    {
        String newStr = input.replaceAll("[, . : ;\"]", "");
        newStr = newStr.replaceAll("\\p{P}","");
        newStr = newStr.replaceAll("\t","");
        return newStr;
    }
    
    // Convierte el archivo de texto de entrada a un HashMap y también guarda la salida final como archivos CSV.
    public  HashMap<String, Integer> getTermsFromFile(String Filename,int count,File folder) {
        HashMap<String,Integer> WordCount = new HashMap<String,Integer>();
        BufferedReader reader = null;
        HashMap<String, Integer> finalMap = new HashMap<>();
        try
        {
            reader = new BufferedReader(new FileReader(Filename));
            String line = reader.readLine();
            while(line!=null)
            {
                String[] words = line.toLowerCase().split(" ");
                for(String term : words)
                {
                    term = cleanseInput(term);
                    if(isDigit(term))
                    {
                        continue;
                    }
                    if(term.length() == 0)
                    {
                        continue;
                    }
                    wordList.add(term);
                    if(WordCount.containsKey(term))
                    {
                        WordCount.put(term,WordCount.get(term)+1);
                    }
                    else
                    {
                        WordCount.put(term,1);
                    }
                }
                line = reader.readLine();
            }
            Map<String, Integer> treeMap = new TreeMap<>(WordCount);
            finalMap = new HashMap<String, Integer>(treeMap);
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
        return finalMap;
    }


    public static void main(String Args[]) throws IOException, InterruptedException, ExecutionException {
        System.out.print("Ingresa la ruta para los archivos de entrada: ");
        Scanner scan = new Scanner(System.in);
        Ejemplo TfidfObj = new Ejemplo();
        File folder = new File(scan.nextLine());
        File[] listOfFiles = folder.listFiles(new FileFilter() {
            @Override
            public boolean accept(File file) {
                return !file.isHidden();
            }
        });

        // Lista para almacenar los CompletableFuture
        List<CompletableFuture<DocumentProperties>> futures = new ArrayList<>();

        for (File file : listOfFiles) {
            if (file.isFile()) {
                CompletableFuture<DocumentProperties> future = CompletableFuture.supplyAsync(() -> {
                    DocumentProperties docProps = new DocumentProperties();
                    HashMap<String, Integer> wordCount = TfidfObj.getTermsFromFile(file.getAbsolutePath(), 0, folder);
                    docProps.setWordCountMap(wordCount);
                    HashMap<String, Double> termFrequency = TfidfObj.calculateTermFrequency(docProps.DocWordCounts);
                    docProps.setTermFreqMap(termFrequency);
                    return docProps;
                });

                futures.add(future);
            }
        }

        // Espera a que todas las tareas se completen
        CompletableFuture<Void> allOf = CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
        allOf.join();

        // Recuperar los resultados
        DocumentProperties[] docProperties = futures.stream()
                .map(CompletableFuture::join)
                .toArray(DocumentProperties[]::new);
    
    
    
        //calculating InverseDocument frequency
        HashMap<String,Double> inverseDocFreqMap = TfidfObj.calculateInverseDocFrequency(docProperties);

        //Calculo del tf-idf
        int count = 0;
        for (File file : listOfFiles) {
            if (file.isFile()) {
                HashMap<String,Double> tfIDF = new HashMap<>();
                double tfIdfValue = 0.0;
                double idfVal = 0.0;
                HashMap<String,Double> tf = docProperties[count].getTermFreqMap();
                Iterator itTF = tf.entrySet().iterator();
                while (itTF.hasNext()) {
                    Map.Entry pair = (Map.Entry)itTF.next();
                    double tfVal  = (Double)pair.getValue() ;
                    if(inverseDocFreqMap.containsKey((String)pair.getKey()))
                    {
                         idfVal = inverseDocFreqMap.get((String)pair.getKey());
                    }
                    tfIdfValue = tfVal *idfVal;
                    tfIDF.put((pair.getKey().toString()),tfIdfValue);
                }
                int fileNameNumber = (count+1);
                String OutPutPath = folder.getAbsolutePath()+"/csvTF-IDF"+file.getName()+fileNameNumber+".csv";
                TfidfObj.outputAsCSV(tfIDF,OutPutPath);
                count++;
            }
        }
    }
}