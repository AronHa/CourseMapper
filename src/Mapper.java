import java.util.ArrayList;
import java.util.Hashtable;

/**
 * A tool to help the faculty at Eastern Mennonite University create Course Maps, using the Engineering
 * Deprtment's course map (found at http://emu.edu/engineering/flowchart.pdf) as inspiration
 * @author Aron Harder
 * @since 2017-19-01
 */

public class Mapper {
	final static int MAX_HOURS = 18; //The max number of credits per semester
	final static int MIN_HOURS = 12; //The minimum number of credits per semester
	final static int NUM_SEMESTERS = 8; //The number of semesters in the schedule
	
	private Course[][] semesters; //The semesters
	private Course[][][] maps; //The final course map
	private int[] total_courses = new int[7]; //Counts how many courses are in each mapping, to find the best mapping
	
	int start_year; //The year of the first semester

	public Course[][] create_mapping(Course[] courses, Requirement[] reqs){
		if (courses.length == 0 || reqs.length == 0){
			return null; //If there are no courses or no requirements, there can be no mapping
		}
		semesters = new Course[NUM_SEMESTERS][MAX_HOURS]; //Initialize the arrays
		maps = new Course[7][NUM_SEMESTERS][MAX_HOURS];
				
		Course[] major = new Course[courses.length]; //Find only the courses needed for the major
		int index = 0;
		start_year = courses[0].get_year(); //Find the start year (the minimum year in courses)
		for (Course c : courses){
			if (c.get_year() < start_year){
				start_year = c.get_year();
			}
		}
		int[] pointers = new int[NUM_SEMESTERS]; //Points to the next open position in each semester array
		for (int i = 0; i < reqs.length; i++){ //Turn each requirement into a course to take
			for (String str : reqs[i].get_courses()){
				Course c = find_course(courses,str); //Find the course outlined in the requirement
				if (c == null){
					continue; //There was a required course that doesn't exist
					//return null;
				} else if (c.get_year()-start_year == 0 && c.get_fall() == false){
					continue; //The course is before the start year, can't add it
				}
				major[index] = c; //Add that course to the major
				if (reqs[i].get_courses().length > 1){
					major[index].set_aorb(true); //If you could take Course A or B, note that
				}
				int sem = (c.get_year()-start_year)*2; //Find the first semester this course can be taken
				if (c.get_fall() == false){
					sem--;
				}
				while (sem < semesters.length){ //Add the course to each possible semester it can be taken
					semesters[sem][pointers[sem]] = c;
					pointers[sem]++;
					sem+=c.get_freq();
				}
				index++;
			}
		}
		
		for (int a = 0; a < maps.length; a++){ //Make a different schedule for each cross cultural semester
			Course[][] sem_clone = new Course[NUM_SEMESTERS][MAX_HOURS]; //need a fresh semesters array for each attempt
			for (int b = 0; b  < semesters.length; b++){ //Copy information from semesters into the clone
				sem_clone[b] = semesters[b].clone();
			}
			Hashtable<String,Boolean> taken_courses = new Hashtable<String,Boolean>(); //Initialize a hashmap to check if a course has been taken
			for (Requirement r : reqs){
				for (String s : r.get_courses()){
					taken_courses.put(s, false);
				}
			}
			for (int i = 0; i < sem_clone.length; i++){ //Go through the semesters from first to last
				if (i == a+2){ //The cross cultural semester
					maps[a][i][0] = new Course(15,"CC101","Cross Cultural",2016,true,1,"Core",new String[]{"sophomore"});
					continue;
				}
				//Remove classes that are before their prereqs
				Course[] to_add = new Course[sem_clone[i].length]; //These are the courses that could be taken this semester
				int to_add_index = 0;
				int credits = 0;
				for (int j = 0; j < sem_clone[i].length && sem_clone[i][j] != null; j++){
					ArrayList<String> prereqs = sem_clone[i][j].get_prereqs(); //Get the prereq list
					int num_prereqs = prereqs.size(); //Need to fulfill all the prereqs
					for (String req : prereqs){
						if (req.equals("freshman")){ //Should take as a freshman
							num_prereqs--;
						} else if (req.equals("sophomore") && i >= 2){ //Need to be at least a sophomore
							num_prereqs--;
						} else if (req.equals("junior") && i >= 4){ //Need to be at least a junior
							num_prereqs--;
						} else if (req.equals("senior") && i >= 6){ //Need to be at least a senior
							num_prereqs--;
						} else { //Has a course prereq
							String[] aorb = req.split(" or "); //Prereqs that say take a or b will be in the form "MATH101 or MATH110"
							for (String str : aorb){
								if (taken_courses.get(str) != null && taken_courses.get(str) == true){ //If we've taken that prereq
									num_prereqs--;
									break;
								}
							}
						}
					}
					if (num_prereqs == 0){ //If we've taken all the prereqs for a course
						to_add[to_add_index] = sem_clone[i][j]; //Add the course to taken_courses, but do it after the semester is over
						credits+=sem_clone[i][j].get_sh();
						to_add_index++;
					} else {
						sem_clone[i][j] = null; //Remove the course from this semester
					}
				}
				
				//Only 18 Credits per semester
				while (credits > MAX_HOURS){ //While there are too many credits in the semester
					int remove = 0; //The index of the course to remove
					int old_priority = 127; //The priority of the course (low number => can take the class later)
					for (int j = 0; j < to_add_index; j++){ //Go through the courses in the semester
						int priority = to_add[j].get_sh(); //Find the course's priority
						if (to_add[j].get_group().equals("Core")){
							priority-=1; //core classes aren't quite so important to take
						}
						for (String s : to_add[j].get_prereqs()){
							if (s.equals("freshman")){
								priority+=4; //freshman-level classes are very important to take
							}
						}
						if (priority < old_priority){
							remove = j;
							old_priority = priority;
						}
					}
					credits-=to_add[remove].get_sh(); //take those credits out of the semester
					to_add[remove] = null; //remove the course from the to_add array
					clean_array(to_add); //left shift to fill the gap
					to_add_index--;
				}
				while (to_add_index > 0){ //Add all of the courses in to_add to taken_courses
					taken_courses.put(to_add[--to_add_index].get_code(),true);
					maps[a][i][to_add_index] = to_add[to_add_index];
					total_courses[a]++;
				}
				clean_array(sem_clone[i]); //Remove the empty spaces between courses
				
				//Remove taken courses from future semesters
				for (int j = i; j < sem_clone.length; j++){
					for (int k = 0; k < sem_clone[j].length && sem_clone[j][k] != null; k++){
						if (taken_courses.get(sem_clone[j][k].get_code()) == true){ //We took this course, remove it
							sem_clone[j][k] = null;
						} else if (sem_clone[j][k].is_aorb() == true){ //Check if we fulfilled this course's requirement
							boolean found = false;
							for (Requirement r : reqs){ //Find the requirement for that course
								for (String s : r.get_courses()){
									if (s.equals(sem_clone[j][k].get_code())){
										found = true;
										break;
									}
								}
								int x_of_y = 0;
								if (found == true){ //If such a requirement exists, find all the courses mentioned in it
									for (String s : r.get_courses()){
										if (taken_courses.get(s) == true){
											x_of_y++;
										}
									}
									break;
								}
								if (x_of_y > r.get_num()){ //If we have taken enough courses to fulfill the requirement, remove this course
									sem_clone[j][k] = null;
									break;
								}
							}
						}
					}
					clean_array(sem_clone[j]); //Remove the empty spaces between courses
				}
			}
		}
		
		//Find the semester with a cross cultural that eliminates no courses
		int best = 0;
		for (int a = 1; a < total_courses.length; a++){
			if (total_courses[a] > total_courses[best]){
				best = a;
			}
		}
		
		return maps[best];
	}
	
	public Course[] network_flow_mapping(Course[] courses, Requirement[] reqs){
		start_year = courses[0].get_year(); //Find the start year (the minimum year in courses)
		for (Course c : courses){
			if (c.get_year() < start_year){
				start_year = c.get_year();
			}
		}

		int[] nodes = new int[courses.length+reqs.length+NUM_SEMESTERS+2];
		double[][] edges = new double[nodes.length][nodes.length];
		for (int i = 0; i < nodes.length; i++){
			nodes[i] = i; //Initialize the courses as nodes, where the number of a node refers to the index of the Course in courses
			//source = nodes[nodes.length-2], sink = nodes[nodes.length-1]
			if (i < courses.length){ //Only for courses, not source(s) or sink
				int sub_sink = (courses[i].get_year()-start_year)*2;
				if (courses[i].get_fall() == false){
					sub_sink--;
				}
				while (sub_sink < NUM_SEMESTERS){
					edges[i][sub_sink+courses.length+reqs.length] = courses[i].get_sh(); //Attach courses to sinks
					sub_sink+=courses[i].get_freq();
				}
				//edges[i][nodes.length-1] = Double.POSITIVE_INFINITY; //Attach courses to sink
				/**if (courses[i].get_prereqs().size() == 0){ //Attach courses without prereqs to sink
					edges[i][nodes.length-1] = Double.POSITIVE_INFINITY;
				} else { //Attach courses with prereqs to their prereq
					for (String p : courses[i].get_prereqs()){
						for (int j = 0; j < courses.length; j++){
							if (courses[j].get_code().equals(p)){
								edges[i][j] = ((double) courses[i].get_sh())/courses[i].get_prereqs().size();
							}
						}
					}
				}*/
			} else if (i < courses.length+reqs.length){ //Only for requirements
				int in_flow = 0;
				for (String r : reqs[i-courses.length].get_courses()){
					for (int j = 0; j < courses.length; j++){
						if (courses[j].get_code().equals(r)){
							edges[i][j] = courses[j].get_sh();
							in_flow+=courses[j].get_sh();
							break;
						}
					}
				}
				edges[nodes.length-2][i] = in_flow;
			} else if (i < nodes.length-2){
				edges[i][nodes.length-1] = MAX_HOURS;
			}
		}
		int[] to_take = max_flow(nodes,edges);
		Course[] output = new Course[to_take.length];
		for (int i = 0; i < to_take.length; i++){
			output[i] = courses[to_take[i]];
		}
		return output;
	}
	private int[] max_flow(int[] nodes, double[][] edges){
		int source = nodes.length-2; //The index of the source
		int sink = nodes.length-1; //The index of the sink
		double[][] origin = new double[nodes.length][];
		for (int i = 0; i < nodes.length; i++){
			origin[i] = edges[i].clone(); //Copy the edges coming out of the sub-sinks for later
		}
		double[][] flow = new double[edges.length][edges[0].length];
		for (int i = 0; i < flow.length; i++){
			for (int j = 0; j < flow[i].length; j++){
				//Initialize the flows
				if (edges[i][j] == 0){
					flow[i][j] = -1; //Not an edge, no flow in this direction
				} else {
					flow[i][j] = 0; //Initial flow is 0
				}
			}
		}
		
		for (int i = 0; i < edges.length; i++){
			for (int j = 0; j < edges[i].length; j++){
				if (j != 0){
					System.out.print(" ");
				}
				System.out.print(edges[i][j]);
			}
			System.out.println("");
			System.out.println("=============");
		}

		int[] path = find_path(nodes,edges,source,sink);
		while (path != null){
			double bottleneck = Double.POSITIVE_INFINITY;
			for (int i = 0; i < path.length-1 && path[i+1] != -1; i++){
				if (edges[path[i]][path[i+1]] < bottleneck){
					bottleneck = edges[path[i]][path[i+1]];
				}
			}
			flow = augment(flow,path,bottleneck);
			for (int i = 0; i < path.length-1 && path[i+1] != -1; i++){
				edges[path[i]][path[i+1]]-=bottleneck;
				edges[path[i+1]][path[i]]+=bottleneck;
			}
			path = find_path(nodes,edges,source,sink);
		}
		
		for (int i = 0; i < flow.length; i++){
			for (int j = 0; j < flow[i].length; j++){
				if (j != 0){
					System.out.print(" ");
				}
				System.out.print(flow[i][j]);
			}
			System.out.println("");
			System.out.println("-------------");
		}
		
		int capacity = 0;
		int vf = 0;
		int[] temp_output = new int[nodes.length];
		int oindex = 0;
		for (int i = nodes.length-2-NUM_SEMESTERS; i < origin.length-1; i++){
			capacity+=MAX_HOURS;
			for (int j = 0; j < origin[i].length; j++){
				if (flow[j][i] != -1){
					vf+=flow[j][i];
					if (flow[j][i] == origin[j][i]){
						temp_output[oindex] = j;
						oindex++;
					}
				}
			}
		}
		int[] output = new int[oindex];
		for (int i = 0; i < oindex; i++){
			output[i] = temp_output[i];
		}
		System.out.println("C: "+capacity);
		System.out.println("v(f): "+vf);
		System.out.println();
		return output;
	}

	private int[] find_path(int[] nodes, double[][] edges, int start, int end){ //BFS
		boolean[] discovered = new boolean[nodes.length];
		int[][] layers = new int[nodes.length][nodes.length];
		int[][] path = new int[nodes.length][nodes.length];
		for (int i = 0; i < nodes.length; i++){
			discovered[i] = false;
			for (int j = 0; j < nodes.length; j++){
				layers[i][j] = -1;
				path[i][j] = -1;
			}
		}
		discovered[start] = true;
		layers[0][0] = start;
		path[start][0] = start;
		int lindex = 0;
		while (layers[lindex][0] != -1){
			int index2 = 0;
			for (int i = 0; i < layers[lindex].length && layers[lindex][i] != -1; i++){
				for (int j = 0; j < edges[layers[lindex][i]].length; j++){
					if (edges[layers[lindex][i]][j] != 0 && discovered[j] == false){
						//System.out.println(layers[lindex][i]+" "+j);
						discovered[j] = true;
						path[j] = path[layers[lindex][i]].clone();
						for (int k = 0; k < path[j].length; k++){
							if (path[j][k] == -1){
								path[j][k] = j;
								break;
							}
						}
						layers[lindex+1][index2] = j;
						index2++;
						if (j == end){
							return path[end];
						}
					}
				}
			}
			lindex++;
		}
		return null; //No path found
	}
	private double[][] augment(double[][] flow, int[] path, double bottleneck){
		double[][] flow2 = new double[flow.length][];
		for (int f = 0; f < flow.length; f++){
			flow2[f] = flow[f].clone();
		}
		for (int i = 0; i < path.length-1 && path[i+1] != -1; i++){
			if (flow2[path[i]][path[i+1]] == -1){ //Is a backward edge
				flow2[path[i+1]][path[i]]-=bottleneck;
			} else {
				flow2[path[i]][path[i+1]]+=bottleneck;
			}
		}
		return flow2;
	}
	
	/**
	 * Returns start year, the year of the first semester
	 * @return start_year
	 */
	public int get_start_year(){
		return start_year;
	}
	
	/**
	 * Changes the start year, if needed
	 * @param new_start_year
	 */
	public void set_start_year(int new_start_year){
		start_year = new_start_year;
	}
	
	/**
	 * Looks for a course
	 * @param array - the courses to look through
	 * @param code - the course code to find
	 * @return the course with that course code
	 */
	public Course find_course(Course[] array, String code){
		for (int i = 0; i < array.length; i++){
			if (array[i] == null){
				break;
			}
			if (array[i].get_code().equals(code)){
				return array[i];
			}
		}
		return null; //If the course isn't found
	}
	/**
	 * Removes gaps in an array
	 * @param array
	 * @return array
	 */
	private Course[] clean_array(Course[] array){
		//Remove the empty spaces between courses
		for (int j = 0; j < array.length; j++){
			boolean b = false;
			if (array[j] == null){ //If there's an empty space
				for (int k = j; k < array.length; k++){
					if (array[k] != null){ //Find the next non-empty space
						array[j] = array[k]; //Move that course into the empty space
						array[k] = null; //Make that course an empty space
						b = true; //There might still be courses to move
						break; //Move on to the next empty space
					}
				}
				if (b == false){ //If there are no courses following this empty space
					break; //We're done
				}
			}
		}
		return array;
	}
}
