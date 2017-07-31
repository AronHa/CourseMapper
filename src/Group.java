import java.awt.Color;
import java.util.ArrayList;

/**
 * Groups of courses for the Display Class
 * @author Aron Harder
 * @since 2017-03-07
 */
public class Group {
	private String name; //The group name
	private ArrayList<Course> courses = new ArrayList<Course>(); //The courses in this group
	private Color color; //The color of the group
	private int[] to_display = new int[]{0,0}; //Which courses to display
	
	private final int MAX_DISPLAY = 16; //The maximum number of courses to display
	
	/**
	 * Creates a dummy group
	 */
	public Group(){
		name = "Name";
		color = new Color(0,128,255);
	}
	/**
	 * Creates a group
	 * @param init_name
	 * @param init_color
	 */
	public Group(String init_name, Color init_color){
		name = init_name;
		color = init_color;
		courses.add(new Course(0,"Display","Add Course",2016,true,1,name,new String[]{}));
	}
	
	/**
	 * Method to change the group name
	 * @param new_name
	 * @return name
	 */
	public String set_name(String new_name){
		name = new_name;
		return name;
	}
	/**
	 * Accessor to get the name of the group
	 * @return name
	 */
	public String get_name(){
		return name;
	}
	/**
	 * Method to change the group color
	 * @param new_color
	 * @return color
	 */
	public Color set_color(Color new_color){
		color = new_color;
		return color;
	}
	/**
	 * Accessor to get the color of the group
	 * @return name
	 */
	public Color get_color(){
		return color;
	}
	/**
	 * Method to add a course
	 * @param new_course
	 */
	public void add_course(Course new_course){
		if (courses.size() == 0){
			courses.add(new_course);
		} else {
			courses.add(courses.size()-1,new_course);
		}
		to_display[1]++;
		int extras = 0;
		if (to_display[0] == 0){ //First set can have 1 extra in it
			extras++;
		}
		if (courses.size()-1 <= to_display[0]+MAX_DISPLAY+extras){
			extras++;
		}
		 if (to_display[1] > to_display[0]+MAX_DISPLAY+extras-1){ //If our interval is too big
			 to_display[1] = to_display[0]+MAX_DISPLAY+extras-1; //Make it the proper size
		} else if (to_display[1] > courses.size()-1){
			to_display[1] = courses.size()-1;
		}
	}
	/**
	 * Method to remove a course
	 * @param to_remove
	 */
	public void remove_course(Course to_remove){
		courses.remove(to_remove);
		if (to_display[1] == courses.size()-2){
			to_display[1] = courses.size()-1;
		} else if (to_display[1] >= courses.size()){
			to_display[1]--;
		}
		if (to_display[1] == to_display[0]){
			int temp = to_display[1];
			decrement_display();
			to_display[1] = temp;
		}
	}
	/**
	 * Accessor to get the courses in the group
	 * @return courses
	 */
	public ArrayList<Course> get_courses(){
		return courses;
	}
	/**
	 * Calculate which courses to display if the user clicks the "previous" button
	 */
	public void decrement_display(){
		if (to_display[0] > 0){
			to_display[0]-=MAX_DISPLAY;
			to_display[1] = to_display[0]+MAX_DISPLAY-1;
			if (to_display[0] == 1){
				to_display[0] = 0;
			}
			if (to_display[1] > courses.size()-1){
				to_display[1] = courses.size()-1;
			}
		}
	}
	/**
	 * Calculate which courses to display if the user clicks the "next" button
	 */
	public void increment_display(){
		if (to_display[1] < courses.size()-2){
			if (to_display[0] == 0){
				to_display[0]++;
			}
			to_display[0]+=MAX_DISPLAY;
			to_display[1] = to_display[0]+MAX_DISPLAY-1;
			if (to_display[1] >= courses.size()-2){
				to_display[1] = courses.size()-1;
			}
		}
	}
	/**
	 * Accessor to get which courses to display
	 * @return to_display
	 */
	public int[] get_display(){
		return to_display;
	}

}
