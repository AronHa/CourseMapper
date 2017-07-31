import java.util.ArrayList;

/**
 * Used to keep track of the courses that will go into the course map
 * @author Aron Harder
 * @since 2017-19-01
 */

public class Course {
	private int sh; //Study Hours
	private String code; //The course code, such as "CS340"
	private String name; //The course name, such as "Calculus III"
	private int year; //The year this course is next offered
	private boolean fall; //Whether this course is in fall (otherwise it's in spring)
	private int freq; //How often the class is offered; 1 = every semester, 2 = every year, 4 = every other year
	private String group; //What group the course is in (determined by user)
	private ArrayList<String> prereqs; //The course prerequisites
	private boolean is_aorb = false; //Whether this course is part of a take X of Y requirement
	private boolean warn = false; //Whether this course has a problem within the schedule
	
	/**
	 * Creates a dummy course with uninitialized values
	 */
	public Course(){
		sh = 3;
		code = "CORE000";
		name = "Course Name";
		year = 2016;
		fall = true;
		freq = 1;
		group = "Core";
		prereqs = new ArrayList<String>();
	}
	/**
	 * Creates a course with user-input values
	 * @param init_sh
	 * @param init_code
	 * @param init_name
	 * @param init_year
	 * @param init_fall
	 * @param init_freq
	 * @param init_group
	 * @param init_prereqs
	 */
	public Course(int init_sh, String init_code, String init_name, int init_year, boolean init_fall,
			int init_freq, String init_group, String[] init_prereqs){
		sh = init_sh;
		code = init_code;
		name = init_name;
		year = init_year;
		fall = init_fall;
		freq = init_freq;
		group = init_group;
		prereqs = new ArrayList<String>();
		for (String req : init_prereqs){
			if (!req.equals("")){
				prereqs.add(req);
			}
		}
	}
	
	/**
	 * A complete initialization for loading courses from file
	 * @param init_sh
	 * @param init_code
	 * @param init_name
	 * @param init_year
	 * @param init_fall
	 * @param init_freq
	 * @param init_group
	 * @param init_prereqs
	 * @param init_aorb
	 * @param init_warn
	 */
	public Course(int init_sh, String init_code, String init_name, int init_year, boolean init_fall,
			int init_freq, String init_group, String[] init_prereqs, boolean init_aorb, boolean init_warn){
		sh = init_sh;
		code = init_code;
		name = init_name;
		year = init_year;
		fall = init_fall;
		freq = init_freq;
		group = init_group;
		prereqs = new ArrayList<String>();
		for (String req : init_prereqs){
			if (!req.equals("")){
				prereqs.add(req);
			}
		}
		is_aorb = init_aorb;
		warn = init_warn;
	}

	/**
	 * Method to change sh if needed
	 * @param new_sh
	 * @return sh
	 */
	public int set_sh(int new_sh){
		sh = new_sh;
		return sh;
	}
	/**
	 * Accessor to get the sh of the course
	 * @return sh
	 */
	public int get_sh(){
		return sh;
	}
	/**
	 * Method to change the course code if needed
	 * @param new_code
	 * @return code
	 */
	public String set_code(String new_code){
		code = new_code;
		return code;
	}
	/**
	 * Accessor to get the course code
	 * @return code
	 */
	public String get_code(){
		return code;
	}
	/**
	 * Method to change the course name if needed
	 * @param new_name
	 * @return name
	 */
	public String set_name(String new_name){
		name = new_name;
		return name;
	}
	/**
	 * Accessor to get the course name
	 * @return name
	 */
	public String get_name(){
		return name;
	}
	/**
	 * Method to change year if needed
	 * @param new_year
	 * @return year
	 */
	public int set_year(int new_year){
		year = new_year;
		return year;
	}
	/**
	 * Accessor to get the year of the course
	 * @return year
	 */
	public int get_year(){
		return year;
	}
	/**
	 * Method to change the semester if needed
	 * @param is_fall
	 * @return fall
	 */
	public boolean set_fall(boolean is_fall){
		fall = is_fall;
		return fall;
	}
	/**
	 * Returns true if the course is offered in the fall. False otherwise
	 * @return fall
	 */
	public boolean get_fall(){
		return fall;
	}
	/**
	 * Method to change frequency if needed
	 * @param new_freq
	 * @return freq
	 */
	public int set_freq(int new_freq){
		freq = new_freq;
		return freq;
	}
	/**
	 * Accessor to get the frequency of the course
	 * @return freq
	 */
	public int get_freq(){
		return freq;
	}
	/**
	 * Method to add a prereq if needed
	 * @param new_prereq
	 */
	/**
	 * Method to give the course a group
	 * @param new_group
	 * @return group
	 */
	public String set_group(String new_group){
		group = new_group;
		return group;
	}
	/**
	 * Accessor to return the course's group
	 * @return is_aorb
	 */
	public String get_group(){
		return group;
	}
	/**
	 * Method to add a prereq if needed
	 * @param new_prereq
	 */
	public void add_prereq(String new_prereq){
		prereqs.add(new_prereq);
	}
	/**
	 * Method to remove a prereq if needed
	 * @param to_remove
	 */
	public void remove_prereq(String to_remove){
		prereqs.remove(to_remove);
	}
	/**
	 * Method to set the prereqs if needed
	 * @param new_prereqs
	 * @return prereqs
	 */
	public ArrayList<String> set_prereqs(String[] new_prereqs){
		prereqs = new ArrayList<String>();
		for (String req : new_prereqs){
			prereqs.add(req);
		}
		return prereqs;
	}
	/**
	 * Accessor to get the prereqs of the course
	 * @return prereqs
	 */
	public ArrayList<String> get_prereqs(){
		return prereqs;
	}
	/**
	 * Method to make the course an aorb course
	 * @param new_aorb
	 * @return is_aorb
	 */
	public boolean set_aorb(boolean new_aorb){
		is_aorb = new_aorb;
		return is_aorb;
	}
	/**
	 * Accessor to return whether the course is an aorb course
	 * @return is_aorb
	 */
	public boolean is_aorb(){
		return is_aorb;
	}
	/**
	 * Method to make the course display a warning
	 * @param new_warn
	 * @return warn
	 */
	public boolean set_warn(boolean new_warn){
		warn = new_warn;
		return warn;
	}
	/**
	 * Accessor to return whether the course should display a warning
	 * @return warn
	 */
	public boolean should_warn(){
		return warn;
	}
}
