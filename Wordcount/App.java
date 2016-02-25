package k3.k3count;

import java.io.IOException;
import java.util.StringTokenizer;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;


public class App 
{
    
    public static class MyMapper extends Mapper<LongWritable, Text, Text, IntWritable>{
        
        private final static IntWritable one = new IntWritable(1);
        private static Text word = new Text();        
        
        @Override
        protected void map(LongWritable key, Text value, Context context)
                throws IOException, InterruptedException {
            
        	Configuration conf = context.getConfiguration();
        	String option = conf.get("options");
        	
        	System.out.println(option);
        	
            StringTokenizer itr = new StringTokenizer(value.toString().toLowerCase().replaceAll("[-+.^:,';’()?!]","")); 
            
            if(option.equals("noisewords")){
            	while(itr.hasMoreTokens()){
            		String tmp = itr.nextToken();
                    if(tmp.equals("and") || tmp.equals("or") || tmp.equals("the") || tmp.equals("to") || tmp.equals("for") || tmp.equals("this") || tmp.equals("that") || tmp.equals("a") ){
                    	word.set("noisewords");
                    } else{
                		word.set(tmp);
                    }
                    context.write(word, one);
                }
            } else if(option.equals("total")){
            	while(itr.hasMoreTokens()){
            		itr.nextToken();
                    word.set("total");
                    context.write(word, one);
                }
            } else {
            	while(itr.hasMoreTokens()){
                    word.set(itr.nextToken());
                    context.write(word, one);
                }
            }
            

        }
        
    }
    
    public static class MyReducer extends Reducer<Text, IntWritable, Text, IntWritable>{
        
        private IntWritable result = new IntWritable();

        
        protected void reduce(Text key, Iterable<IntWritable> values,
                Context context) throws IOException, InterruptedException {
    
            int sum = 0;
            
            for (IntWritable val : values){
                sum += val.get();
            }
            
            result.set(sum);
            context.write(key, result);
    
        }
    }
    

    public static void main(String[] args) throws IOException,
            ClassNotFoundException, InterruptedException
    {

        Configuration conf = new Configuration();
        
        if(args.length == 3){
            conf.set("options", args[2]);
        } else {
        	conf.set("options", "none");
        }
        
        Job job = Job.getInstance(conf, "Word Count example");
        
        job.setJarByClass(App.class);
        job.setMapperClass(MyMapper.class);
        job.setReducerClass(MyReducer.class);
        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(IntWritable.class);
        
        FileInputFormat.addInputPath(job, new Path(args[0]));
        
        FileSystem fs = FileSystem.get(conf);
       
       if (fs.exists(new Path(args[1]))) {
           fs.delete(new Path(args[1]), true);
       }
       
       FileOutputFormat.setOutputPath(job, new Path(args[1]));
       
       System.exit(job.waitForCompletion(true) ? 0 : 1);
    }
    
}