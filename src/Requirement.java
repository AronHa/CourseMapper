/**
 * Used to create requirements for the various majors
 * @author Aron Harder
 * @since 2017-04-02
 */

public class Requirement {
	private String[] courses; //Given by the course codes
	private int num_to_take; //The number of courses on that list that need to be taken
	
	/**
	 * Creates a dummy requirement
	 */
	public Requirement(){
		courses = new String[]{};
		num_to_take = 0;
	}
	/**
	 * Creates the requirement "take course x"
	 */
	public Requirement(String course){
		courses = new String[]{course};
		num_to_take = 1;
	}
	/**
	 * Creates the requirement "take x of these courses"
	 */
	public Requirement(String[] init_courses, int init_num_to_take){
		courses = init_courses;
		num_to_take = init_num_to_take;
	}

	/**
	 * Method to change the courses if needed
	 * @param new_courses
	 * @return courses
	 */
	public String[] set_courses(String[] new_courses){
		courses = new_courses;
		return courses;
	}
	/**
	 * Accessor to get the list of courses
	 * @return year
	 */
	public String[] get_courses(){
		return courses;
	}
	/**
	 * Method to change the number of courses to take
	 * @param new_num_to_take
	 */
	public int set_num(int new_num_to_take){
		num_to_take = new_num_to_take;
		return num_to_take;
	}
	/**
	 * Accessor to get the number of courses to take
	 * @return num_to_take
	 */
	public int get_num(){
		return num_to_take;
	}
}
